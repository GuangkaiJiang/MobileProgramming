package com.example.tipview

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class Dice(private val numSides: Int) {

    fun roll(): Int {
        return (1..numSides).random()
    }
}


class MainActivity : AppCompatActivity() {
    private lateinit var tipOptions: RadioGroup
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logoutButton: Button = findViewById(R.id.button_logout)
        logoutButton.setOnClickListener {
            // Sign out the current user from Firebase
            FirebaseAuth.getInstance().signOut()

            // Now check if the user is signed in with Google
            val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
            if (googleSignInAccount != null) {
                // Ensure the Google Sign In Client is initialized
                if(::mGoogleSignInClient.isInitialized) {
                    // They are signed in with Google, so sign them out.
                    mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                        // Handle sign out (update your UI or start your sign in activity)
                        updateUI(null)
                    }
                } else {
                    // Log the error or handle initialization
                    Log.e(TAG, "GoogleSignInClient not initialized")
                    // You may want to reinitialize it here or show an error message
                }
            } else {
                // Not signed in with Google, just update the UI
                updateUI(null)
            }
        }

        tipOptions = findViewById(R.id.tip_options)
        val rollButton: Button = findViewById(R.id.button)
        rollButton.setOnClickListener {
            rollDice()
        }

        val calculateButton: Button = findViewById(R.id.calculate_button)
        calculateButton.setOnClickListener {
            calculateTotalAmount()
        }

        loadLastFivePayments()

    }

    private fun updateUI(user: FirebaseUser?) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the activity stack
        startActivity(intent)
        //finish() // Finish this activity
    }


    private fun calculateTotalAmount() {
        val costOfServiceEditText: EditText = findViewById(R.id.cost_of_service)
        val roundUpSwitch: Switch = findViewById(R.id.round_up_switch)

        val costOfService = costOfServiceEditText.text.toString().toDoubleOrNull()
        if (costOfService == null) {
            Toast.makeText(this, "Please enter a valid cost of service", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTipPercentage = when (tipOptions.checkedRadioButtonId) {
            R.id.option_30_percent -> 0.30
            R.id.option_25_percent -> 0.25
            R.id.option_20_percent -> 0.20
            R.id.option_15_percent -> 0.15
            R.id.option_10_percent -> 0.10
            R.id.option_5_percent -> 0.05
            else -> {
                Toast.makeText(this, "Please select a tip option", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())

        var totalAmount = costOfService + (costOfService * selectedTipPercentage)
        if (roundUpSwitch.isChecked) {
            totalAmount = kotlin.math.ceil(totalAmount)
        }

        // Display the totalAmount to the user
        Toast.makeText(this, "Total Amount: $totalAmount", Toast.LENGTH_SHORT).show()

        val paymentDetails = hashMapOf(
            "original_cost" to costOfService,
            "tip" to (costOfService * selectedTipPercentage),
            "rounded_up" to roundUpSwitch.isChecked,
            "total_amount" to totalAmount,
            "timestamp" to timestamp // Adding the timestamp here
        )

        val db = Firebase.firestore
        db.collection("payments").document("helloworld")
            .set(paymentDetails)



    }

    private fun rollDice() {
        val dice = Dice(6)
        val diceRoll = dice.roll()

        val resultImageView: ImageView = findViewById(R.id.Dice)
        val img = when (diceRoll) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            else -> R.drawable.dice_6
        }
        resultImageView.setImageResource(img)

        val selectedRadioButton = when (diceRoll) {
            1 -> R.id.option_5_percent
            2 -> R.id.option_10_percent
            3 -> R.id.option_15_percent
            4 -> R.id.option_20_percent
            5 -> R.id.option_25_percent
            else -> R.id.option_30_percent
        }
        tipOptions.check(selectedRadioButton)
    }

    private fun loadLastFivePayments() {
        val db = Firebase.firestore
        db.collection("payments")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by timestamp descending
            .limit(5) // Limit to 5 documents
            .get()
            .addOnSuccessListener { documents ->
                val paymentsList = mutableListOf<String>()
                for (document in documents) {
                    val totalAmount = document.getDouble("total_amount")
                    totalAmount?.let { paymentsList.add(it.toString()) }
                }
                updatePaymentsDisplay(paymentsList)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun updatePaymentsDisplay(paymentsList: List<String>) {
        val previousPaymentsTextView: TextView = findViewById(R.id.previous_payments)
        previousPaymentsTextView.text = "Last 5 payments: ${paymentsList.joinToString(", ")}"
    }


}