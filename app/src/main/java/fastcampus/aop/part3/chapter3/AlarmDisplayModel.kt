package fastcampus.aop.part3.chapter3

data class AlarmDisplayModel(
    val hour: Int,
    val minute: Int,
    var onOff: Boolean
) {

    // 시간 표시
    val timeText: String
        get() {
            // 12시 보다 작으면 그대로 (오전) , 크면 -12 , 화면에 14시 말고 2시로 표시 (오후)
            val h = "%02d".format(if (hour < 12) hour else hour - 12)
            val m = "%02d".format(minute)

            return "$h:$m"
        }

    // 12시보다 작으면 am , 크면 pm
    val ampmText: String
        get() {
            return if (hour < 12) "AM" else "PM"
        }

    // onOff 값이 true면 알람끄기로 , 아닐경우에는 켜기
    val onOffText: String
        get() {
            return if (onOff) "알람 끄기" else "알람 켜기"
        }

    // 시간 데이터 매서드 , 리턴값 xx:xx
    fun makeDataForDB(): String {
        return "$hour:$minute"
    }

}