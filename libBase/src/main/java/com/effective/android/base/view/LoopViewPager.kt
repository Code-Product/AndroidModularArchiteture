package com.effective.android.base.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.annotation.NonNull
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

import java.util.ArrayList

/**
 * 可循环滑动的viewpager
 * Created by yummyLau on 2018/7/02.
 * Email: yummyl.lau@gmail.com
 */
class LoopViewPager<T> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {

    var adapter: BannerPageAdapter<T>? = null

    private val mOnPageChangeListeners = ArrayList<OnPageChangeListener>()
    private var manualTurning = true
    private var autoTurning = true
    private var turningDuration = 3400
    private val smoothScrollDuration = 400

    private var mScroller: InternalScroller? = null

    private val mHandler = InterHandler()


    override fun setCurrentItem(item: Int) {
        super.setCurrentItem(adapter!!.toPosition(item))
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        super@LoopViewPager.setCurrentItem(adapter!!.toPosition(item), smoothScroll)
    }

    override fun getCurrentItem(): Int {
        return adapter!!.toRealPosition(super.getCurrentItem())
    }


    /**
     * dispatch all PageChange events
     */
    private val proxyRootListener = object : OnPageChangeListener {

        private var mPreviousPosition = -1

        override fun onPageSelected(position: Int) {
            val realPosition = adapter!!.toRealPosition(position)
            if (mPreviousPosition != realPosition) {
                mPreviousPosition = realPosition
                for (listener in mOnPageChangeListeners) {
                    listener.onPageSelected(realPosition)
                }
            }
        }

        override fun onPageScrolled(position: Int, positionOffset: Float,
                                    positionOffsetPixels: Int) {
            val realPosition = adapter!!.toRealPosition(position)
            for (listener in mOnPageChangeListeners) {
                listener.onPageScrolled(realPosition, positionOffset, positionOffsetPixels)
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager.SCROLL_STATE_IDLE || state == ViewPager.SCROLL_STATE_DRAGGING) {
                if (super@LoopViewPager.getCurrentItem() === adapter!!.getCount() - 1) {
                    super@LoopViewPager.setCurrentItem(1, false)
                } else if (super@LoopViewPager.getCurrentItem() === 0) {
                    super@LoopViewPager.setCurrentItem(adapter!!.getCount() - 2, false)
                }
            }
            for (listener in mOnPageChangeListeners) {
                listener.onPageScrollStateChanged(state)
            }
        }
    }

    init {
        super.addOnPageChangeListener(proxyRootListener)
        try {
            val scrollField = ViewPager::class.java!!.getDeclaredField("mScroller")
            scrollField.setAccessible(true)
            mScroller = InternalScroller(getContext())
            mScroller!!.duration = smoothScrollDuration
            scrollField.set(this, mScroller)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (adapter != null) {
            requestLayout()
        }
        startTuring()
    }

    private fun startTuring() {
        if (mHandler.hasMessages(MSG_AUTO_TURNING)) {
            mHandler.removeMessages(MSG_AUTO_TURNING)
        }
        mHandler.sendEmptyMessageDelayed(MSG_AUTO_TURNING, turningDuration.toLong())
    }

    private fun stopTurning() {
        if (mHandler.hasMessages(MSG_AUTO_TURNING)) {
            mHandler.removeMessages(MSG_AUTO_TURNING)
        }
    }


    fun setViewHolder(holder: ViewHolder<T>) {
        adapter = BannerPageAdapter(holder)
        super.setAdapter(adapter)
    }

    fun setData(data: List<T>?) {
        if (adapter == null) {
            throw IllegalStateException("setViewHolder must be called first")
        }
        if (data == null || data.size == 0) {
            return
        }
        adapter!!.setData(data)
        setCurrentItem(0, false)
        startTuring()
    }

    fun setAutoTurning(autoTurning: Boolean) {
        this.autoTurning = autoTurning
        if (autoTurning) {
            startTuring()
        } else {
            stopTurning()
        }
    }


    fun setManualTurning(manualTurning: Boolean) {
        this.manualTurning = manualTurning
    }


    fun setTurningDuration(turningDuration: Int) {
        this.turningDuration = turningDuration
    }

    fun setSmoothScrollDuration(smoothScrollDuration: Int) {
        mScroller!!.duration = smoothScrollDuration
    }


    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            startTuring()
        } else {
            stopTurning()
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (manualTurning) {
            super.onTouchEvent(ev)
        } else {
            false
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (manualTurning) {
            super.onInterceptTouchEvent(ev)
        } else {
            false
        }
    }


    override fun addOnPageChangeListener(@NonNull listener: OnPageChangeListener) {
        mOnPageChangeListeners.add(listener)
    }


    override fun removeOnPageChangeListener(@NonNull listener: OnPageChangeListener) {
        mOnPageChangeListeners.remove(listener)
    }

    override fun clearOnPageChangeListeners() {
        mOnPageChangeListeners.clear()
    }

    override fun setOnPageChangeListener(onPageChangeListener: OnPageChangeListener) {
        mOnPageChangeListeners.add(onPageChangeListener)
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
            if (autoTurning) {
                startTuring()
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            if (autoTurning) {
                stopTurning()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    @JvmOverloads
    override fun setPageTransformer(reverseDrawingOrder: Boolean, transformer: PageTransformer?, pageLayerType: Int) {
        super.setPageTransformer(reverseDrawingOrder, transformer, pageLayerType)
        if (transformer == null) {
            offscreenPageLimit = 1
        } else {
            offscreenPageLimit = 100
        }
    }

    private class InternalScroller : Scroller {
        private var scrollDuration = 1000

        constructor(context: Context) : super(context) {}

        constructor(context: Context, interpolator: Interpolator) : super(context, interpolator) {}

        constructor(context: Context, interpolator: Interpolator, flywheel: Boolean) : super(context, interpolator, flywheel) {}

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            super.startScroll(startX, startY, dx, dy, scrollDuration)
        }

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
            super.startScroll(startX, startY, dx, dy, scrollDuration)
        }

        fun setDuration(duration: Int) {
            scrollDuration = duration
        }

    }

    @SuppressLint("HandlerLeak")
    private inner class InterHandler : Handler() {

        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_AUTO_TURNING) {
                //no data or only one
                if (adapter == null || adapter!!.getCount() === 0 || adapter!!.getCount() === 1 || !autoTurning) {
                    stopTurning()
                    return
                }
                try {
                    val item = super@LoopViewPager.getCurrentItem()
                    super@LoopViewPager.setCurrentItem(item + 1, true)
                    mHandler.sendEmptyMessageDelayed(MSG_AUTO_TURNING, turningDuration.toLong())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    companion object {
        private val TAG = "LoopViewPager"
        private val MSG_AUTO_TURNING = 0X520
    }

}


class BannerPageAdapter<R>(var viewHolder: ViewHolder<R>) : PagerAdapter() {

    var mData: MutableList<R> = ArrayList()
    private var once: Boolean = false

    override fun getCount(): Int {
        return mData.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = viewHolder.getView(container.context, toRealPosition(position), mData[position])
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        container.removeView(view)
    }


    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    fun setData(data: List<R>) {
        mData.clear()
        mData.addAll(data)
        once = data.size == 1
        if (!once) {
            val first = data[0]
            val last = data[data.size - 1]
            mData.add(first)
            mData.add(0, last)
        }
        notifyDataSetChanged()
    }


    /**
     * switch from the position in ViewPager to
     * the position in user's eyes
     */
    fun toRealPosition(position: Int): Int {
        if (once || mData.size == 0)
            return 0
        return if (position == count - 1) {
            0
        } else if (position == 0) {
            count - 3
        } else {
            position - 1
        }
    }

    /**
     * switch from the position in user's eyes to
     * the position in ViewPager
     */
    fun toPosition(realPosition: Int): Int {
        //only one or never set data
        return if (once || mData.size == 0) {
            0
        } else realPosition + 1
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }
}


interface ViewHolder<T> {
    fun getView(context: Context, position: Int, data: T): View
}