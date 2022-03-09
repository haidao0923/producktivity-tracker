package com.example.producktivity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), BillingProcessor.IBillingHandler {

    lateinit var sharedPreferences: SharedPreferences

    var countdown : CountDownTimer ?= null
    var currentTime : Int = 0
    var currentDuckName : String = "Your First Duck (Tap here to rename)"
    var timeSettingArray: Array<Int> = arrayOf(1, 5, 10, 15, 20, 25, 30, 45, 60, 75, 90, 120)
    var startTime: Long = System.currentTimeMillis()
    var dateOfYear: Int = java.util.Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    var weekOfYear: Int = java.util.Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
    var timeSetting: Int = 1
    var isRunning: Boolean = false
    var isCollapsed: Boolean = false

    var boughtNoAds: Boolean = false

    open var ducks = mutableMapOf<String, Duck>()

    lateinit var mAdView : AdView

    lateinit var bp:BillingProcessor
    var license: String = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnSt5bOh+Ya7mhkAYtQ3YX405D+sznmrPP+rbC67AA6ikawiQ6Y57VH8vrs6Yyz8s/Dw88cLEAhsQTKN3Sjlpb4XQy+1j8U96ZUadtcLw1XJwgNkrnjcaJhvhy0+5dQ+eXBfHVsHdSItf1mNPx/kz6kYJ+FpADwmHDYsZDkZu+A6KJp2nfKCulQKZADdJb993VkP/M07PY/zY10+h77AY94AsD5QKzyx+2/7FZRN3b+3XkkCug1q3yz4DzMGjMJY9rrmoUwX6Ah1i0yJMj+3v68ItvNKRvfZb8rWuKocINfjT+8YM2WY87MYDQACxWQcAfGwcyZxReTmB8mB2U+1fDwIDAQAB"

    override fun onBillingInitialized() {

    }

    override fun onPurchaseHistoryRestored() {
        removeAds()
    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        removeAds()
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!bp.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        if(bp!=null)
            bp.release()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        load()

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        if (boughtNoAds) {
            removeAds()
        }

        bp = BillingProcessor(this, license, this)
        bp.initialize()

        if (ducks.size == 0) {
            ducks["Your First Duck (Tap here to rename)"] = Duck("Your First Duck (Tap here to rename)")
        }
        sort(DuckCompareTimeToday)

        findViewById<Button>(R.id.start_button).setOnClickListener {
            if (!isRunning){
                if (findViewById<Spinner>(R.id.duck_dropdown).selectedItem == null) {
                    Toast.makeText(applicationContext, "Choose a duck!", Toast.LENGTH_SHORT).show()
                } else if (findViewById<Spinner>(R.id.time_dropdown).selectedItem == null) {
                    Toast.makeText(applicationContext, "Select a time!", Toast.LENGTH_SHORT).show()
                } else {
                    timeSetting = findViewById<Spinner>(R.id.time_dropdown).selectedItem.toString().toInt()
                    startCounting(
                        it,
                        findViewById<Spinner>(R.id.duck_dropdown).selectedItem.toString(),
                        timeSetting * 60
                    )
                }
            } else{
                stopCounting(it)
            }
        }
    }

//Save and Load
    var list: List<Duck> = emptyList()

    fun buyButton(view: View) {
        bp.purchase(this, "no_ads")
    }

    private fun removeAds() {
        boughtNoAds = true
        mAdView.destroy()
        findViewById<LinearLayout>(R.id.linearLayout).removeView(mAdView)
        save()
    }


    fun saveJustTime() {
        val editor: SharedPreferences.Editor = sharedPreferences.edit();
        editor.putInt("CurrentTime", currentTime).apply()
        editor.putLong("LastTime", System.currentTimeMillis()).apply()
    }

    fun save() {
        val mutableList: MutableList<Duck> = mutableListOf()
        mutableList.addAll(ducks.values)
        list = Collections.unmodifiableList(mutableList)
        val editor: SharedPreferences.Editor = sharedPreferences.edit();
        editor.putString("DuckList", Gson().toJson(list)).apply()
        editor.putString("CurrentDuckName", currentDuckName).apply()
        editor.putBoolean("IsRunning", isRunning).apply()
        editor.putInt("TimeSetting", timeSetting).apply()
        editor.putInt("DateOfYear", dateOfYear).apply()
        editor.putInt("WeekOfYear", weekOfYear).apply()
        editor.putLong("StartTime", startTime).apply()
        editor.putBoolean("BoughtNoAds", boughtNoAds).apply()
    }

    fun loadButton(view: View) {
        load()
    }

    inline fun <reified T> genericType() = object: TypeToken<T>() {}.type
    fun load() {
        val duckList = genericType<List<Duck>>()
        val mutableList: MutableList<Duck> = mutableListOf()
        mutableList.addAll(ducks.values)
        list = Collections.unmodifiableList(mutableList)
        val list: List<Duck> = Gson().fromJson(
            sharedPreferences.getString(
                "DuckList", Gson().toJson(
                    list
                )
            ), duckList
        )
        list.forEach {
            ducks[it.name] = it
        }
        currentDuckName = sharedPreferences.getString(
            "CurrentDuckName",
            "Your First Duck (Tap here to rename)"
        ).toString()
        if (dateOfYear != sharedPreferences.getInt("DateOfYear", 0)) {
            ducks.values.forEach {
                it.time_today = 0;
            }
        }
        if (weekOfYear != sharedPreferences.getInt("WeekOfYear", 0)) {
            ducks.values.forEach {
                it.this_week = 0;
                it.timeEachDayOfWeek = Array(7) { i -> 0}
            }
        }
        timeSetting = sharedPreferences.getInt("TimeSetting", 0)
        isRunning = sharedPreferences.getBoolean("IsRunning", false)
        if (isRunning) {
            val timePassed = (System.currentTimeMillis() - sharedPreferences.getLong("LastTime", 0)) / 1000
            val tempCurrentTime = sharedPreferences.getInt("CurrentTime", 0)
            if (timePassed >= tempCurrentTime) {
                finishCountdown(findViewById(R.id.start_button))
            } else {
                startCounting(
                    findViewById(R.id.start_button),
                    currentDuckName,
                    (tempCurrentTime - timePassed).toInt()
                )
            }
            saveJustTime()
        }
        startTime = sharedPreferences.getLong("StartTime", System.currentTimeMillis())
        boughtNoAds = sharedPreferences.getBoolean("BoughtNoAds", false)
        sort(DuckCompareTimeToday)
    }
//End of Save and Load

    fun collapseTable(view: View) {
        val table = findViewById<TableLayout>(R.id.tableLayout)
        val switchBtn = findViewById<Button>(R.id.switchBtn)

        // setColumnCollapsed(int columnIndex, boolean isCollapsed)
        table.setColumnCollapsed(1, !isCollapsed);
        table.setColumnCollapsed(2, !isCollapsed);
        table.setColumnCollapsed(3, !isCollapsed);
        table.setColumnCollapsed(4, isCollapsed);
        table.setColumnCollapsed(5, isCollapsed);
        table.setColumnCollapsed(6, isCollapsed);
        table.setColumnCollapsed(7, isCollapsed);
        table.setColumnCollapsed(8, isCollapsed);
        table.setColumnCollapsed(9, isCollapsed);
        table.setColumnCollapsed(10, isCollapsed);
        table.setColumnCollapsed(11, !isCollapsed);
        table.setColumnCollapsed(12, !isCollapsed);

        if (isCollapsed) {
            // Close
            isCollapsed = false;
            switchBtn.setText("Show Detail");
        } else {
            // Open
            isCollapsed = true;
            switchBtn.setText("Hide Detail");
        }
    }

    fun sort(comparator: Comparator<Duck>) {
        val table = findViewById<TableLayout>(R.id.tableLayout)
        table.removeViews(1, table.childCount - 1)

        var list: List<Duck> = ducks.values.toList()
        list = list.sortedWith(comparator).sortedBy {!it.isActive}
        list.forEach {
            sortDuck(it)
        }
        updateDuckDropdown(list)
        updateTimeSettingDropdown()
        save()
    }

    fun sortDuck(duck: Duck) {
        val tableLayout: TableLayout = findViewById(R.id.tableLayout);
        val tableRow: TableRow = getLayoutInflater().inflate(R.layout.tablerow, null) as TableRow;

        var tv: TextView
        var cb: CheckBox

        //Filling in cells
        tv = tableRow.findViewById(R.id.duck_name)
        tv.setText(duck.name)

        tv = tableRow.findViewById(R.id.time_today)
        tv.setText(duck.getTimeString(duck.time_today))

        tv = tableRow.findViewById(R.id.this_week)
        tv.setText(duck.getTimeString(duck.this_week))

        tv = tableRow.findViewById(R.id.all_time)
        tv.setText(duck.getTimeString(duck.all_time))

        tv = tableRow.findViewById(R.id.sunday)
        tv.setText(duck.getTimeString(duck.timeEachDayOfWeek[0]))
        tv = tableRow.findViewById(R.id.monday)
        tv.setText(duck.getTimeString(duck.timeEachDayOfWeek[1]))
        tv = tableRow.findViewById(R.id.tuesday)
        tv.setText(duck.getTimeString(duck.timeEachDayOfWeek[2]))
        tv = tableRow.findViewById(R.id.wednesday)
        tv.setText(duck.getTimeString(duck.timeEachDayOfWeek[3]))
        tv = tableRow.findViewById(R.id.thursday)
        tv.setText(duck.getTimeString(duck.timeEachDayOfWeek[4]))
        tv = tableRow.findViewById(R.id.friday)
        tv.setText(duck.getTimeString(duck.timeEachDayOfWeek[5]))
        tv = tableRow.findViewById(R.id.saturday)
        tv.setText(duck.getTimeString(duck.timeEachDayOfWeek[6]))

        cb = tableRow.findViewById(R.id.active)
        cb.isChecked = duck.isActive
        //Add row to the table
        tableLayout.addView(tableRow);
    }

    fun addDuck(view: View, name: String) {
        val duck: Duck = Duck(name)
        ducks[name] = duck
        sort(DuckCompareTimeToday)
    }

    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val checked: Boolean = view.isChecked

            when (view.id) {
                R.id.active -> {
                    ducks[(view.parent.parent as ViewGroup).findViewById<TextView>(R.id.duck_name).text.toString()]!!.isActive =
                        checked
                    sort(DuckCompareTimeToday)
                }
                R.id.delete -> {
                    createDeleteAlert(view)
                }
            }
        }
    }

    fun createRenameAlert(view: View) {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Rename ${(view as TextView).text}")
// Set up the input
        val input = EditText(this)
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        builder.setView(input)
// Set up the buttons
        builder.setPositiveButton("Confirm",
            DialogInterface.OnClickListener { dialog, _ ->
                val oldDuck = ducks[(view as TextView).text.toString()]!!
                val newDuckName: String = input.text.trim().toString()
                if (currentDuckName == oldDuck.name) {
                    currentDuckName = newDuckName
                }
                ducks[newDuckName] = oldDuck
                ducks.remove(oldDuck.name)
                ducks[newDuckName]!!.name = newDuckName
                sort(DuckCompareTimeToday)
            })
        builder.setNegativeButton(
            android.R.string.cancel,
            DialogInterface.OnClickListener { dialog, _ -> dialog.cancel() })

        val dialog = builder.show()
        if (input.text.trim().isEmpty()) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        input.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (input.text.trim().isEmpty()) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                } else dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !ducks.containsKey(
                    input.text.trim().toString()
                )
            }
        })
    }

    fun createAddAlert(view: View) {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Name your duck!")
// Set up the input
        val input = EditText(this)
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        builder.setView(input)
// Set up the buttons
        builder.setPositiveButton("Confirm",
            DialogInterface.OnClickListener { dialog, which ->
                addDuck(view, input.text.trim().toString())
            })
        builder.setNegativeButton(
            android.R.string.cancel,
            DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

        val dialog = builder.show()
        if (input.text.trim().isEmpty()) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        input.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (input.text.trim().isEmpty()) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                } else dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !ducks.containsKey(
                    input.text.trim().toString()
                )
            }
        })
    }

    fun createDeleteAlert(view: View) {
        val view = view.parent.parent
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle("Confirm Delete")
        val duckName: String = (view as ViewGroup).findViewById<TextView>(R.id.duck_name).text.toString()
        builder.setMessage("Are you sure you want to delete\n$duckName?\nYou cannot undo this action.")
        builder.setPositiveButton("Confirm",
            DialogInterface.OnClickListener { dialog, which ->
                if (currentDuckName == duckName) {
                    stopCounting(view)
                }
                ducks.remove(duckName)
                (view.parent as ViewGroup).removeView(view as ViewGroup)
                sort(DuckCompareTimeToday)
            })
        builder.setNegativeButton(
            android.R.string.cancel,
            DialogInterface.OnClickListener { dialog, which -> })

        val dialog: android.app.AlertDialog? = builder.create()
        if (dialog != null) {
            dialog.show()
        }
    }

    fun updateDuckDropdown(list: List<Duck>) {
        var duckDropdown: Spinner = findViewById(R.id.duck_dropdown);
        var spinnerList: ArrayList<String> = ArrayList()
        var thisIndex: Int = 0
        var inactiveElements: Int = 0
        var listSize: Int = 0

        list.forEach { element ->
            if (element.name == currentDuckName) {
                thisIndex = listSize
            }
            if (!element.isActive) {
                inactiveElements += 1
            }
            spinnerList.add(listSize, element.name)
            listSize += 1
        }
        listSize -= inactiveElements
        val arrayAdapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            spinnerList
        ) {
            override fun getCount(): Int {
                return listSize
            }
        }
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        duckDropdown.setAdapter(arrayAdapter)
        duckDropdown.setSelection(thisIndex)

    }

    fun updateTimeSettingDropdown() {
        var timeSettingDropdown: Spinner = findViewById(R.id.time_dropdown);
        var spinnerList: ArrayList<Int> = ArrayList()
        var thisIndex: Int = 0

        timeSettingArray.forEachIndexed { index, element ->
            spinnerList.add(element)
            if (element == timeSetting) {
                thisIndex = index
            }
        }

        var arrayAdapter: ArrayAdapter<Int> = ArrayAdapter<Int>(
            this,
            android.R.layout.simple_spinner_item,
            timeSettingArray
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSettingDropdown.setAdapter(arrayAdapter)
        timeSettingDropdown.setSelection(thisIndex)
    }

    private fun stopCounting(view: View) {
        findViewById<Button>(R.id.start_button).text = "Start Training"
        if (isRunning) {
            isRunning = false
            countdown!!.cancel()
        }

        val tempTime = timeSetting * 60
        val minute = tempTime / 60
        val second = tempTime % 60
        val textString = String.format("%02d", minute) + ":" + String.format("%02d", second)
        findViewById<TextView>(R.id.timer).text = textString

        findViewById<Spinner>(R.id.duck_dropdown).isEnabled = true
        findViewById<Spinner>(R.id.time_dropdown).isEnabled = true
        save()
    }

    private fun startCounting(view: View, tempDuckName: String, secondsToCount: Int) {

        findViewById<Button>(R.id.start_button).text = "Stop Training"
        isRunning = true
        var timeDropDown = (view.parent as ViewGroup).findViewById<Spinner>(R.id.time_dropdown)
        timeDropDown.isEnabled = false
        currentTime = secondsToCount
        countdown = object  : CountDownTimer(currentTime.toLong() * 1000, 1000){
            override fun onFinish() {
                finishCountdown(view)
            }

            override fun onTick(millisUntilFinished: Long) {
                currentTime -= 1
                var minute = currentTime / 60
                var second = currentTime % 60
                var textString = String.format("%02d", minute) + ":" + String.format("%02d", second)
                findViewById<TextView>(R.id.timer).text = textString
                saveJustTime()
            }
        }.start()
        var duckDropdown = (view.parent as ViewGroup).findViewById<Spinner>(R.id.duck_dropdown)
        duckDropdown.isEnabled = false
        currentDuckName = tempDuckName
        save()
    }

    private fun finishCountdown(view: View) {
        findViewById<TextView>(R.id.timer).text = "Done!"
        (view as TextView).text = "Start Training"
        isRunning = false

        val tempDate = sharedPreferences.getInt("DateOfYear", 0)
        if (dateOfYear != tempDate) {
            ducks.values.forEach {
                it.time_today = 0;
            }
        }
        val tempWeek = sharedPreferences.getInt("WeekOfYear", 0)
        if (weekOfYear != tempWeek) {
            ducks.values.forEach {
                it.this_week = 0;
                it.timeEachDayOfWeek = Array(7) { i -> 0}
            }
        }

        if (ducks.containsKey(currentDuckName)) {
            ducks[currentDuckName]!!.time_today += timeSetting
            ducks[currentDuckName]!!.this_week += timeSetting
            ducks[currentDuckName]!!.all_time += timeSetting
            ducks[currentDuckName]!!.timeEachDayOfWeek[Calendar.getInstance()
                .get(Calendar.DAY_OF_WEEK) - 1] += timeSetting
        }
        sort(DuckCompareTimeToday)

        (view.parent as ViewGroup).findViewById<Spinner>(R.id.duck_dropdown).isEnabled = true
        (view.parent as ViewGroup).findViewById<Spinner>(R.id.time_dropdown).isEnabled = true
    }
}