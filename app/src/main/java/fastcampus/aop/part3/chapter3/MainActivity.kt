package fastcampus.aop.part3.chapter3

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
import java.util.*

class MainActivity : AppCompatActivity() {


    /** 알람매니저를 등록하고 시간이되면 노티를 생성해서 띄우는 형식으로 구현되어있는데
     * 알람 매니저는 많이 쓰기 때문에 안드로이드 문서를 보고 직접 구현을 다시 해봐야 겠다
     * 노티도 생각보다 앱에서 많이 사용하기때문에 , 구현방법을 꼭 숙지하자
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // onoff 버튼 초기화
        initOnOffButton()

        // 시간 재설정 버튼 초기화
        initChangeAlarmTimeButton()

        // 기본 저장된 값을 가져옴
        val model = fetchDataFromSharedPreferences()

        // 가져온 값을 화면에 표시
        renderView(model)

    }

    private fun initOnOffButton() {
        // 알람 켜기 버튼 초기화
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {

            // 버튼 tag에 저장된값이 없으면 종료 , model 데이터가 있으면 val model에 대입
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            // .not 은 !와 같은 의미 , 사용자가 지정한 값으로 저장 model.onOff.not()을 하는 이유는 켜기에서 클릭 -> 끄기 , 끄기에서 클릭 -> 켜기 구현
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            // 값 입력
            renderView(newModel)

            // onOff값이 on일 경우 (알람 설정일경우)
            if (newModel.onOff) {
                // 켜진 경우 -> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    // 캘린더에 설정된값 가져오기
                    set(Calendar.HOUR_OF_DAY, newModel.hour) // 시간
                    set(Calendar.MINUTE, newModel.minute) // 분

                    // 현재 설정된값이랑 기존에 캘린더 값이랑 비교 이후면 true 이전이면 false(현재 시간)
                    // 변경된값은 현재 시간보다 이전인가? yes -> true 당일에 울림 , no -> false 하루 뒤에 울림
//                    {Calendar 개체}.after({비교대상})
//                    이 기본 문법인데, 이것을 이해하는 방법은 아래와 같다.
//                    {Calendar 개체} after then {비교대상}
//                    즉, “{비교대상}보다 {Calendar 개체}가 이후 인가?”라는 의미와 같다.

                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                // 알람 매니저 객체 생성 as로 형변환
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this, ALARM_REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT
                )

                // setRepeating은 상대적으로 배터리 소모가 심하지만 정확하고 setInexactRepeating은 부정확하고 오차범위가 존재하지만 배터리소모가 적다
                // RTC_WAKEUP	지정된 시간에 기기의 절전 모드를 해제하여 대기 중인 인텐트를 실행합니다.
                // calendar.timeInMillis ~ 하루 간격으로 반복
                // pendingIntent 전달
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                // 알람 취소일 경우
            } else {
                cancelAlarm()
            }

        }
    }

    private fun initChangeAlarmTimeButton() {
        // 시간 재설정 초기화
        val changeAlarmButton = findViewById<Button>(R.id.changeAlarmTimeButton)
        changeAlarmButton.setOnClickListener {

            // 캘린더 객체 생성
            val calendar = Calendar.getInstance()

            // 시간 서정 다이어로그 띄우기 ,람다 함수 구현
            TimePickerDialog(this, { picker, hour, minute ->

                // 시간값 저장
                val model = saveAlarmModel(hour, minute, false)

                // 시간 재설정시 알람on일경우 off로 바꾸고 알람 취소
                renderView(model)

                // TimePickerDialog 형식 설정 마지막 파라미터는 24시 형식으로 보여줄지 표시 false로 해야 3:30  이렇게 나옴
                cancelAlarm()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()

        }

    }

    // 설정값 저장 하기 반환값은 moel
    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        //sharedPreferences.edit()를 with에 전달해서 값 초기화
        with(sharedPreferences.edit()) {
            // 알람키, onOff키를 넣고 데이터 저장
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }

        return model
    }

    // sharedPreferences 로 사용자가 설정한값 AlarmDisplayModel 리턴
    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        // 시간
        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "9:30"
        // 알람 on,off 설정
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)
        // 시간 데이터 기본값은 9:30
        val alarmData = timeDBValue.split(":")
        // AlarmDisplayModel 생성
        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        // 보정 보정 예외처리  pendingIntent값 불러오기
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
        )

        if ((pendingIntent == null) and alarmModel.onOff) {
            // 알람은 꺼져있는데, 데이터는 켜저있는 경우
            alarmModel.onOff = false

        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            // 알람은 켜져있는데, 데이터는 꺼져있는 경우
            // 알람을 취소함
            pendingIntent.cancel()
        }

        return alarmModel

    }

    private fun renderView(model: AlarmDisplayModel) {
        // am 인지 pm인지 설정
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }

        // 시간 표시
        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }

        // on off 글씨 변경
        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            // 버튼 태그 안에 모델값 저장
            // 문자열부터, 숫자, 객체, 등 자바의 모든 데이터를 저장할 수 있다.
            tag = model
        }

    }

    // 알람 취소
    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
        )
        pendingIntent?.cancel()
    }

    // 자바로 치면 static
    companion object {
        // sharedPreferences 키값 지정 ,  pendingIntent REQUEST_CODE 지정
        private const val SHARED_PREFERENCES_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000

    }
}