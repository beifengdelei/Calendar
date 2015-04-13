package com.jjc.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 日历控件 功能：获得点选的日期区间
 * 
 */
public class CalendarView extends View implements View.OnTouchListener {
	
	private final static String TAG = "anCalendar";
	
	private Date selectedStartDate;
	private Date selectedEndDate;
	private Date curDate; // 当前日历显示的月
	private Date today; // 今天的日期文字显示红色
	private Date downDate; // 手指按下状态时临时日期
	private boolean preButton = false;//手指按下上月按钮状态
	private boolean nextButton = false;//手指按下下月按钮状态；
	private DateInfo downInfo;// 手指按下状态时临时日期
	private Date showFirstDate, showLastDate; // 日历显示的第一个日期和最后一个日期
	private int downIndex; // 按下的格子索引
	private Calendar calendar;
	private Surface surface;
	private int[] date = new int[42]; // 日历显示数字
	
	//日历42格填充数据
	private ArrayList<DateInfo> datelist = new ArrayList<DateInfo>();
	//当前页面中的行程日期（上一月、当月、下一月）
	private List<DateInfo> playTolist = new ArrayList<DateInfo>();
	//服务器返回行程日期
	private List<DateInfo> playlist = new ArrayList<DateInfo>();
	
	/**
	 * 当前年
	 */
	private int toYear;
	/**
	 * 上一个月
	 */
	private int preMonth;
	/**
	 * 当前月
	 */
	private int toMonth;
	/**
	 * 下一个月
	 */
	private int nextMonth;
	/**
	 * 当前日
	 */
	private int toDay;
	
	
	private int curStartIndex, curEndIndex; // 当前显示的日历起始的索引
	//private boolean completed = false; // 为false表示只选择了开始日期，true表示结束日期也选择了
	
	//给控件设置监听事件
	private OnCellClickListener onCellClickListener;
	private OnButtonClickListener onButtonClickListener;
	
	public CalendarView(Context context) {
		super(context);
		init(context);
	}

	public CalendarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		curDate = selectedStartDate = selectedEndDate = today = new Date();
		calendar = Calendar.getInstance();
		calendar.setTime(curDate);
		
		surface = new Surface();
		//获取当前显示屏的密度
		surface.density = getResources().getDisplayMetrics().density;
		setBackgroundColor(surface.bgColor);
		setOnTouchListener(this);
		
		for (int j = 0; j < 42; j++) {
			DateInfo dateinfo = new DateInfo();
			datelist.add(dateinfo);
		}
		
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		surface.width = getResources().getDisplayMetrics().widthPixels;
		surface.height = (int) (getResources().getDisplayMetrics().heightPixels*3/5);
		widthMeasureSpec = MeasureSpec.makeMeasureSpec(surface.width,
				MeasureSpec.EXACTLY);
		heightMeasureSpec = MeasureSpec.makeMeasureSpec(surface.height,
				MeasureSpec.EXACTLY);
		setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed) {
			surface.init();
		}
		super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected void onDraw(Canvas canvas) {
//		System.out.println("------------"+toYear+"-"+toMonth+"-"+toDay);
		System.out.println("------onDraw------");
		
		// 画框
		canvas.drawPath(surface.boxPath, surface.borderPaint);
		// 取得年月
		String monthText = getYearAndmonth();
		//取得字符串显示的宽度值
		float textWidth = surface.monthPaint.measureText(monthText);
		//在画布相应位置画出日期
		canvas.drawText(monthText, (surface.width - textWidth) / 2f,
				surface.monthHeight * 3 / 4f, surface.monthPaint);
		
		// 画出上一月/下一月按钮
		canvas.drawPath(surface.preMonthBtnPath, surface.monthChangeBtnPaint);
		canvas.drawPath(surface.nextMonthBtnPath, surface.monthChangeBtnPaint);
		drawWeekText(canvas);
		
		// 计算日期-获取ArrayList<DateInfo>值
		calculateDate();
		
		if(!playlist.isEmpty()){
			playTolist.clear();
			//取出服务器返回行程日程表，当前页面中上月、当月、下月中的日程
			for (Iterator iterator = playlist.iterator(); iterator.hasNext();) {
				DateInfo dateInfo = (DateInfo) iterator.next();
				
				if(dateInfo.getYear()== toYear){
					if(dateInfo.getMonth() == preMonth){
						playTolist.add(dateInfo);
					}else if(dateInfo.getMonth() == toMonth){
						playTolist.add(dateInfo);
					}else if(dateInfo.getMonth() == nextMonth){
						playTolist.add(dateInfo);
					}
				}
			}
			//行程日期 与当前页面内日程匹配，如果相同，设置标志;
			for (int i = 0; i < datelist.size(); i++) {
				DateInfo dateInfo = datelist.get(i);
				for(int j = 0; j < playTolist.size(); j++){
					DateInfo playInfo = playTolist.get(j);
					if(playInfo.getMonth()==dateInfo.getMonth() && playInfo.getDay()== dateInfo.getDay()){
						datelist.set(i, playInfo);
						}
					}
				}
		}
		
//		for (Iterator iterator = datelist.iterator(); iterator.hasNext();) {
//			DateInfo type = (DateInfo) iterator.next();
//			System.out.println("-"+type.getYear()+"-"+type.getMonth()+"-"+type.getDay()+"-"+type.isTag());
//		}
		
		drawDownOrSelectedBg(canvas);
		drawDateText(canvas);
		super.onDraw(canvas);
	}

	/**
	 * 根据服务器返回数据，绘制当前页面对应日期行程标识
	 * @param list 所有行程日期
	 */
	public void drawPlayDate(List<DateInfo> list){
		System.out.println("<--list size--"+playlist.size());
		if(!list.isEmpty()){
			playlist.clear();
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				DateInfo dateInfo = (DateInfo) iterator.next();
				playlist.add(dateInfo);
			}
			invalidate();
		}
	}
	
	/**
	 * 计算日期，填充ArrayList<DateInfo>
	 */
	private void calculateDate() {
		
		calendar.setTime(curDate);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		
		toYear = calendar.get(Calendar.YEAR);
		toMonth = calendar.get(Calendar.MONTH) + 1;
		preMonth = calendar.get(Calendar.MONTH);
		nextMonth =calendar.get(Calendar.MONTH) + 2;
		toDay = calendar.get(Calendar.DATE);
		
		int dayInWeek = getFirstWeekdayOfMonth(toYear,toMonth);
		int monthStart = dayInWeek;
		monthStart -= 1;  //以日为开头-1，以星期一为开头-2
		curStartIndex = monthStart;
		
		// last month
		if (monthStart > 0) {
			
			int dayInmonth = getDaysOfMonth(toYear,preMonth);
			for (int i = monthStart - 1; i >= 0; i--) {
				DateInfo dateInfo = new DateInfo();
				dateInfo.setYear(toYear);
				dateInfo.setMonth(preMonth);
				dateInfo.setDay(dayInmonth);
				dateInfo.setTag(false);
				datelist.set(i, dateInfo);
				dayInmonth--;
			}
		}
		
		showFirstDate = calendar.getTime();
		
		// this month
		int monthDay = getDaysOfMonth(toYear,toMonth);
		LogUtil.d(TAG, "- monthDay -"+monthDay);
		for (int i = 0; i < monthDay; i++) {
			date[monthStart + i] = i + 1;
			DateInfo dateinfo1 = new DateInfo();
			dateinfo1.setYear(toYear);
			dateinfo1.setMonth(toMonth);
			dateinfo1.setDay(i+1);
			dateinfo1.setTag(false);
			datelist.set(monthStart + i, dateinfo1);
		}
		
		curEndIndex = monthStart + monthDay;
		
		// next month
		for (int i = monthStart + monthDay; i < 42; i++) {
			DateInfo dateInfo = new DateInfo();
			dateInfo.setYear(toYear);
			dateInfo.setMonth(nextMonth);
			dateInfo.setDay(i - (monthStart + monthDay) + 1);
			dateInfo.setTag(false);
			datelist.set(i, dateInfo);
		}
		
		if (curEndIndex < 42) {
			// 显示了下一月的
			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		showLastDate = calendar.getTime();
	}
	
	/**
	 * 获取每月的第一周
	 * @param year
	 * @param month
	 * @return
	 */
	private int getFirstWeekdayOfMonth(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.SATURDAY); // 星期天为第一天
        c.set(year, month - 1, 1);
        return c.get(Calendar.DAY_OF_WEEK);
    }
	
    /**
     * 获取某年某月的天数
     * @param year
     * @param month
     * @return
     */
    private int getDaysOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * 绘制表格所有的日期
     * @param canvas
     */
    private void drawDateText(Canvas canvas) {
    	// today index
		int todayIndex = -1;
		calendar.setTime(curDate);
		String curYearAndMonth = calendar.get(Calendar.YEAR) + ""+ calendar.get(Calendar.MONTH);
		calendar.setTime(today);
		String todayYearAndMonth = calendar.get(Calendar.YEAR) + ""+ calendar.get(Calendar.MONTH);
		if (curYearAndMonth.equals(todayYearAndMonth)) {
			int todayNumber = calendar.get(Calendar.DAY_OF_MONTH);
			todayIndex = curStartIndex + todayNumber - 1;
		}
    	for (int i = 0; i < 42; i++) {
			int color = surface.textColor;
			if (isLastMonth(i)) {
				color = surface.borderColor;
			} else if (isNextMonth(i)) {
				color = surface.borderColor;
			}
			if (todayIndex != -1 && i == todayIndex) {
				color = surface.todayNumberColor;
			}
			drawCellText(canvas, i, datelist.get(i).getDay() + "", color);
			if(datelist.get(i).isTag()){
				drawCellImge(canvas, i, R.drawable.circle_blue1);
			}
		}
    }
	/**
	 * 绘制表格指定位置的日期
	 * @param canvas
	 * @param index
	 * @param text
	 */
	private void drawCellText(Canvas canvas, int index, String text, int color) {
		int x = getXByIndex(index);
		int y = getYByIndex(index);
		surface.datePaint.setColor(color);
		float cellY = surface.monthHeight + surface.weekHeight + (y - 1)
				* surface.cellHeight + surface.cellHeight * 3 / 4f;
		float cellX = (surface.cellWidth * (x - 1))
				+ (surface.cellWidth - surface.datePaint.measureText(text))
				/ 2f;
		
		canvas.drawText(text, cellX, cellY, surface.datePaint);
	}
	
	/**
	 * 绘制表格中的星期
	 * @param canvas
	 */
	private void drawWeekText(Canvas canvas) {
		// 星期Y轴坐标
		float weekTextY = surface.monthHeight + surface.weekHeight * 3 / 4f;
		
		//星期数组内文字画入表格
		for (int i = 0; i < surface.weekText.length; i++) {
			float weekTextX = i
					* surface.cellWidth
					+ (surface.cellWidth - surface.weekPaint
							.measureText(surface.weekText[i])) / 2f;
			canvas.drawText(surface.weekText[i], weekTextX, weekTextY,
					surface.weekPaint);
		}
	}
	
	/**
	 * 绘制行程标记
	 * @param canvas
	 * @param index
	 * @param imageId
	 */
	private void drawCellImge(Canvas canvas, int index, int imageId){
		int x = getXByIndex(index);
		int y = getYByIndex(index);
		
		// 定义画笔
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        float left = surface.cellWidth * (x - 1) + surface.borderWidth;
		float top = surface.monthHeight + surface.weekHeight + (y - 1)
				* surface.cellHeight + surface.borderWidth;
		canvas.drawBitmap(decodeFile(imageId), left,top,paint);
	}
	
	
	private Bitmap decodeFile(int imageId) {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(getResources().openRawResource(imageId), null, o);
			final int REQUIRED_SIZE = (int)(surface.width*0.1f);
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp / 2 < REQUIRED_SIZE
						|| height_tmp / 2 < REQUIRED_SIZE)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(getResources().openRawResource(imageId), null, o2);
	}

	/**
	 * 绘制表格的背景
	 * @param canvas
	 * @param index
	 * @param color
	 */
	private void drawCellBg(Canvas canvas, int index, int color) {
		int x = getXByIndex(index);
		int y = getYByIndex(index);
		surface.cellBgPaint.setColor(color);
		float left = surface.cellWidth * (x - 1) + surface.borderWidth;
		float top = surface.monthHeight + surface.weekHeight + (y - 1)
				* surface.cellHeight + surface.borderWidth;
		canvas.drawRect(left, top, left + surface.cellWidth
				- surface.borderWidth, top + surface.cellHeight
				- surface.borderWidth, surface.cellBgPaint);
	}
	
	/**
	 * 按下状态，选择状态背景色
	 * @param canvas
	 */
	private void drawDownOrSelectedBg(Canvas canvas) {
		// down and not up
		if (downDate != null) {
			drawCellBg(canvas, downIndex, surface.cellDownColor);
		}
		// selected bg color
		if (!selectedEndDate.before(showFirstDate)
				&& !selectedStartDate.after(showLastDate)) {
			int[] section = new int[] { -1, -1 };
			calendar.setTime(curDate);
			calendar.add(Calendar.MONTH, -1);
			findSelectedIndex(0, curStartIndex, calendar, section);
			if (section[1] == -1) {
				calendar.setTime(curDate);
				findSelectedIndex(curStartIndex, curEndIndex, calendar, section);
			}
			if (section[1] == -1) {
				calendar.setTime(curDate);
				calendar.add(Calendar.MONTH, 1);
				findSelectedIndex(curEndIndex, 42, calendar, section);
			}
			if (section[0] == -1) {
				section[0] = 0;
			}
			if (section[1] == -1) {
				section[1] = 41;
			}
			for (int i = section[0]; i <= section[1]; i++) {
				drawCellBg(canvas, i, surface.cellSelectedColor);
			}
		}
	}

	private void findSelectedIndex(int startIndex, int endIndex,
			Calendar calendar, int[] section) {
		for (int i = startIndex; i < endIndex; i++) {
			calendar.set(Calendar.DAY_OF_MONTH, date[i]);
			Date temp = calendar.getTime();
			// Log.d(TAG, "temp:" + temp.toLocaleString());
			if (temp.compareTo(selectedStartDate) == 0) {
				section[0] = i;
			}
			if (temp.compareTo(selectedEndDate) == 0) {
				section[1] = i;
				return;
			}
		}
	}

	public Date getSelectedStartDate() {
		return selectedStartDate;
	}

	public Date getSelectedEndDate() {
		return selectedEndDate;
	}

	private boolean isLastMonth(int i) {
		if (i < curStartIndex) {
			return true;
		}
		return false;
	}

	private boolean isNextMonth(int i) {
		if (i >= curEndIndex) {
			return true;
		}
		return false;
	}

	private int getXByIndex(int i) {
		return i % 7 + 1; // 1 2 3 4 5 6 7
	}

	private int getYByIndex(int i) {
		return i / 7 + 1; // 1 2 3 4 5 6
	}

	/**
	 * 获得当前应该显示的年月
	 * @return 年-月
	 */
	public String getYearAndmonth() {
		calendar.setTime(curDate);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		return surface.monthText[month]+year;
	}
	
	public void setPreMonth(){
		calendar.setTime(curDate);
		calendar.add(Calendar.MONTH, -1);
		curDate = calendar.getTime();
		
		setState(null, true, false);
	}
	
	public void setNextMonth(){
		calendar.setTime(curDate);
		calendar.add(Calendar.MONTH, 1);
		curDate = calendar.getTime();
		
		setState(null, false, true);
	}

	private void setSelectedDateByCoor(float x, float y) {
		// change month
		if (y < surface.monthHeight) {
			// pre month
			if (x < surface.monthChangeWidth) {
				setPreMonth();
			}
			// next month
			else if (x > surface.width - surface.monthChangeWidth) {
				setNextMonth();
			}
		}
		// cell click down
		if (y > surface.monthHeight + surface.weekHeight) {
			int m = (int) (Math.floor(x / surface.cellWidth) + 1);
			int n = (int) (Math
					.floor((y - (surface.monthHeight + surface.weekHeight))
							/ Float.valueOf(surface.cellHeight)) + 1);
			downIndex = (n - 1) * 7 + m - 1;
			Log.d(TAG, "downIndex:" + downIndex);
			downInfo = datelist.get(downIndex);
			calendar.setTime(curDate);
			if (isLastMonth(downIndex)) {
				calendar.add(Calendar.MONTH, -1);
			} else if (isNextMonth(downIndex)) {
				calendar.add(Calendar.MONTH, 1);
			}
			calendar.set(Calendar.DAY_OF_MONTH, date[downIndex]);
			
			setState(calendar.getTime(), false, false);
		}
//		invalidate();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			setSelectedDateByCoor(event.getX(), event.getY());
			break;
		case MotionEvent.ACTION_UP:
			if (downDate != null && preButton == false && nextButton == false) {
				selectedStartDate = selectedEndDate = downDate;
				//响应日期块监听事件
				onCellClickListener.OnCellClick(downInfo);
				setState(null, false, false);
			}else if (downDate == null && preButton == true && nextButton == false) {
				//当点击上月的时候，返回标志true;
				onButtonClickListener.OnButtonClick(true,curDate);
				setState(null, false, false);
			}else if (downDate == null && preButton == false && nextButton == true) {
				//当点击下月的时候，返回标志false;
				onButtonClickListener.OnButtonClick(false,curDate);
				setState(null, false, false);
			}
			invalidate();
			break;
		}
		return true;
	}
	
	/**
	 * 分别标记上月、下月、日期块点击状态
	 * @param downDate 日期块
	 * @param preButton 上月
	 * @param nextButton 下月
	 */
	private void setState(Date downDate, boolean preButton, boolean nextButton){
		this.downDate = downDate;
		this.preButton = preButton;
		this.nextButton = nextButton;
	}
	
	//给日期块设置监听事件
	public void setOnCellClickListener(OnCellClickListener onCellClickListener){
		this.onCellClickListener =  onCellClickListener;
	}
	//日期接口
	public interface OnCellClickListener {
		void OnCellClick(DateInfo info);
	}
	
	//给上月、下月控件设置监听事件
	public void setOnButtonClickListener(OnButtonClickListener onButtonClickListener){
		this.onButtonClickListener =  onButtonClickListener;
	}
	//上月、下月接口
	public interface OnButtonClickListener {
		void OnButtonClick(boolean tag, Date date);
	}
	
	

	/**
	 * 
	 * 1. 布局尺寸 2. 文字颜色，大小 3. 当前日期的颜色，选择的日期颜色
	 */
	private class Surface {
		
		public float density;
		
		/**
		 * 控件的宽度
		 */
		public int width;
		
		/**
		 * 控件的高度
		 */
		public int height;
		
		/**
		 * 显示月的高度
		 */
		public float monthHeight;
		
		/**
		 * 上一月、下一月按钮宽度
		 */
		public float monthChangeWidth;
		
		/**
		 * 星期的高度
		 */
		public float weekHeight;
		
		/**
		 * 日期方框宽度
		 */
		public float cellWidth;
		
		/**
		 * 日期方框高度	
		 */
		public float cellHeight;
		public float borderWidth;
		public int bgColor = Color.parseColor("#FFFFFF");//控件背景颜色-白色
		
		private int textColor = Color.BLACK;
		private int btnColor = Color.parseColor("#666666");//Button背景颜色
		private int borderColor = Color.parseColor("#CCCCCC");//边界颜色
		
		public int todayNumberColor = Color.RED;//当日字体颜色-红色
		
		public int cellDownColor = Color.parseColor("#CCFFFF");
		public int cellSelectedColor = Color.parseColor("#99CCFF");
		
		public Paint borderPaint;//边框画笔
		public Paint monthPaint;//月份画笔
		public Paint weekPaint;//星期画笔
		public Paint datePaint;//日期画笔
		public Paint monthChangeBtnPaint;
		public Paint cellBgPaint;
		public Path boxPath; // 边框路径
		public Path preMonthBtnPath; // 上一月按钮三角形
		public Path nextMonthBtnPath; // 下一月按钮三角形
		public String[] weekText = getResources().getStringArray(R.array.weekName);
		public String[] monthText = getResources().getStringArray(R.array.monthName);
		public void init() {
			float temp = height / 7f;
			monthHeight = temp;
			
			/**
			 * 月份的宽度
			 */
			monthChangeWidth = monthHeight * 2.5f;
			
			/**
			 * 星期的高度
			 */
			weekHeight = (float) ((temp + temp * 0.3f) * 0.4);
			
			/**
			 * 日期表格的高度
			 */
			cellHeight = (height - monthHeight - weekHeight) / 6f;
			
			/**
			 * 日期表格的宽度
			 */
			cellWidth = width / 7f;
			
			//日历控件最外层边框画笔
			borderPaint = new Paint();
			borderPaint.setColor(borderColor);
			borderPaint.setStyle(Paint.Style.STROKE);//设置画笔风格-空心
			
			borderWidth = (float) (0.5 * density);
			borderWidth = borderWidth < 1 ? 1 : borderWidth;
			borderPaint.setStrokeWidth(borderWidth);//当画笔样式为STROKE或FILL_OR_STROKE时，设置笔刷的粗细度  
			
			//月份画笔
			monthPaint = new Paint();
			monthPaint.setColor(textColor);
			monthPaint.setAntiAlias(true);
			float textSize = cellHeight * 0.6f;
			monthPaint.setTextSize(textSize);
			monthPaint.setTypeface(Typeface.DEFAULT_BOLD);
			
			//星期画笔
			weekPaint = new Paint();
			weekPaint.setColor(textColor);
			weekPaint.setAntiAlias(true);
			float weekTextSize = weekHeight * 0.5f;
			weekPaint.setTextSize(weekTextSize);
			weekPaint.setTypeface(Typeface.DEFAULT_BOLD);
			
			//日期画笔
			datePaint = new Paint();
			datePaint.setColor(textColor);
			datePaint.setAntiAlias(true);
			float cellTextSize = cellHeight * 0.5f;
			datePaint.setTextSize(cellTextSize);
			datePaint.setTypeface(Typeface.DEFAULT_BOLD);

			//表格
			boxPath = new Path();
			boxPath.addRect(0, 0, width, height, Direction.CW);
			boxPath.moveTo(0, monthHeight);
			boxPath.rLineTo(width, 0);
			boxPath.moveTo(0, monthHeight + weekHeight);
			boxPath.rLineTo(width, 0);
			
			for (int i = 1; i < 6; i++) {
				boxPath.moveTo(0, monthHeight + weekHeight + i * cellHeight);
				boxPath.rLineTo(width, 0);
				boxPath.moveTo(i * cellWidth, monthHeight);
				boxPath.rLineTo(0, height - monthHeight);
			}
			boxPath.moveTo(6 * cellWidth, monthHeight);
			boxPath.rLineTo(0, height - monthHeight);
			
			//上一个月标识
			preMonthBtnPath = new Path();
			int btnHeight = (int) (monthHeight * 0.6f);
			preMonthBtnPath.moveTo(monthChangeWidth / 2f, monthHeight / 2f);
			preMonthBtnPath.rLineTo(btnHeight / 2f, -btnHeight / 2f);
			preMonthBtnPath.rLineTo(0, btnHeight);
			preMonthBtnPath.close();
			
			//下一个月标识
			nextMonthBtnPath = new Path();
			nextMonthBtnPath.moveTo(width - monthChangeWidth / 2f,
					monthHeight / 2f);
			nextMonthBtnPath.rLineTo(-btnHeight / 2f, -btnHeight / 2f);
			nextMonthBtnPath.rLineTo(0, btnHeight);
			nextMonthBtnPath.close();
			
			monthChangeBtnPaint = new Paint();
			monthChangeBtnPaint.setAntiAlias(true);
			monthChangeBtnPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			monthChangeBtnPaint.setColor(btnColor);
			
			cellBgPaint = new Paint();
			cellBgPaint.setAntiAlias(true);
			cellBgPaint.setStyle(Paint.Style.FILL);
			cellBgPaint.setColor(cellSelectedColor);
		}
	}
}
