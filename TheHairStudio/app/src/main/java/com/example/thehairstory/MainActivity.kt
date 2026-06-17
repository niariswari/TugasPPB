package com.example.thehairstory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.thehairstory.data.local.AppDatabase
import com.example.thehairstory.data.repository.MembershipRepositoryImpl
import com.example.thehairstory.ui.navigation.NavGraph
import com.example.thehairstory.ui.theme.TheHairStoryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MembershipRepositoryImpl(
            memberDao = database.memberDao(),
            transactionDao = database.transactionDao(),
            serviceDao = database.serviceDao(),
            rewardDao = database.rewardDao()
        )

        lifecycleScope.launch {
            try {
                // Seed services & rewards (only if tables are empty)
                repository.ensureServicesSeeded()
                repository.ensureRewardsSeeded()

                // Seed demo members only if database is empty
                val count = repository.getMemberCount().first()
                if (count == 0) {
                    // MB0001 — Clara Adisutjipto
                    val claraId = repository.registerMember(
                        name = "Clara Adisutjipto",
                        email = "clara@email.com",
                        phone = "081234567890",
                        password = "clara123"
                    ).toInt()
                    repository.addTransaction(claraId, 850000.0, "SV0007 Hair Coloring\nSV0003 Hair Wash & Blow", "2026-06-10", "SV0007,SV0003", "")
                    repository.addTransaction(claraId, 350000.0, "SV0005 Creambath\nSV0003 Hair Wash & Blow", "2026-06-15", "SV0005,SV0003", "")
                    repository.addTransaction(claraId, 600000.0, "SV0008 Keratin Treatment\nSV0001 Haircut", "2026-06-16", "SV0008,SV0001", "")
                    repository.redeemReward(claraId, 100, "RW0001 Potongan Rp25.000", "2026-06-16", "RW0001")

                    // MB0002 — Budi Santoso
                    val budiId = repository.registerMember(
                        name = "Budi Santoso",
                        email = "budi@email.com",
                        phone = "085678901234",
                        password = "budi123"
                    ).toInt()
                    repository.addTransaction(budiId, 180000.0, "SV0002 Haircut\nSV0004 Hair Styling", "2026-06-05", "SV0002,SV0004", "")
                    repository.addTransaction(budiId, 270000.0, "SV0006 Hair Spa\nSV0003 Hair Wash & Blow", "2026-06-12", "SV0006,SV0003", "")

                    // MB0003 — Dewi Lestari
                    val dewiId = repository.registerMember(
                        name = "Dewi Lestari",
                        email = "dewi@email.com",
                        phone = "087890123456",
                        password = "dewi123"
                    ).toInt()
                    repository.addTransaction(dewiId, 1500000.0, "SV0007 Hair Coloring\nSV0008 Keratin Treatment", "2026-05-20", "SV0007,SV0008", "")
                    repository.addTransaction(dewiId, 300000.0, "SV0005 Creambath\nSV0003 Hair Wash & Blow", "2026-06-01", "SV0005,SV0003", "")
                    repository.redeemReward(dewiId, 250, "RW0002 Gratis Hair Wash & Blow", "2026-06-10", "RW0002")
                    repository.addTransaction(dewiId, 800000.0, "SV0009 Hair Smoothing\nSV0001 Haircut", "2026-06-14", "SV0009,SV0001", "")

                    // MB0004 — Mazaya
                    val mazayaId = repository.registerMember(
                        name = "Mazaya",
                        email = "mazaya@email.com",
                        phone = "081298765432",
                        password = "mazaya123"
                    ).toInt()
                    repository.addTransaction(mazayaId, 150000.0, "SV0002 Haircut (Senior Stylist)", "2026-06-17", "SV0002", "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            TheHairStoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        repository = repository
                    )
                }
            }
        }
    }
}
