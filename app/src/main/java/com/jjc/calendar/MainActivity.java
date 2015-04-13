package com.jjc.calendar;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {

    private CalendarView calendar;
    private List<DateInfo> playlist = new ArrayList<DateInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取日历控件对象
        calendar = (CalendarView)findViewById(R.id.calendar);

        calendar.setOnCellClickListener(new calendarCellClickListener());
        calendar.setOnButtonClickListener(new calendarButtonClickListener());

        getData();

        calendar.drawPlayDate(playlist);
    }


    private void getData() {
        // TODO Auto-generated method stub
        playlist.clear();
        //测试填充数据
        for (int i = 0; i < 5; i++) {
            DateInfo dateInfo = new DateInfo();
            dateInfo.setYear(2013);
            dateInfo.setMonth(8);
            dateInfo.setDay(10+i);
            dateInfo.setTag(true);
            playlist.add(dateInfo);
        }
    }

    private class calendarButtonClickListener implements CalendarView.OnButtonClickListener {

        @Override
        public void OnButtonClick(boolean tag, Date date) {
            // TODO Auto-generated method stub
            if(tag){
                getData();
                System.out.println("--palylist size-->"+playlist.size());
                calendar.drawPlayDate(playlist);

//				Toast.makeText(getApplicationContext(), "-上-年"+DateUtil.getYear(date)+"-月-"+DateUtil.getMonth(date), Toast.LENGTH_SHORT).show();
            }else{
                getData();
                System.out.println("--palylist size-->"+playlist.size());
                calendar.drawPlayDate(playlist);
//				Toast.makeText(getApplicationContext(), "-下-年"+DateUtil.getYear(date)+"-月-"+DateUtil.getMonth(date), Toast.LENGTH_SHORT).show();
            }
        }
    }



    private class calendarCellClickListener implements CalendarView.OnCellClickListener {

        @Override
        public void OnCellClick(DateInfo info) {
            // TODO Auto-generated method stub
            Toast.makeText(getApplicationContext(), "--Year--" + info.getYear() + "--Month--" + info.getMonth() + "--Day--" + info.getDay() + "--Tag--" + info.isTag(), Toast.LENGTH_SHORT).show();
        }

    }

}
