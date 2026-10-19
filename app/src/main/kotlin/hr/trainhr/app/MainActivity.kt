package hr.trainhr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import hr.trainhr.app.ui.AppRoot
import hr.trainhr.app.ui.AppViewModel
import hr.trainhr.app.ui.TrainTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrainTheme {
                AppRoot(vm)
            }
        }
    }
}
