
package ro.pub.cs.systems.eim.lab10.siplinphone

import android.widget.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.linphone.core.*

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check the app/build.gradle to see how to import the LibLinphone SDK !!!

        setContentView(R.layout.main_activity)
        val coreVersion = findViewById<TextView>(R.id.core_version)

        // Core is the main object of the SDK. You can't do much without it.
       // To create a Core, we need the instance of the Factory.
        val factory = Factory.instance()

        // Some configuration can be done before the Core is created, for example enable debug logs.
        factory.setDebugMode(true, "Hello Linphone")

        val core = factory.createCore(null, null, this)

        // Now we can start using the Core object
        coreVersion.text = core.version
    }
}
