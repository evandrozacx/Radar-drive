package com.example

import com.example.data.DailyShiftEntity
import com.example.data.DriverSettingsEntity
import com.example.data.ExpenseEntity
import com.example.data.ExpenseType
import com.example.data.RadarRepository
import com.example.data.RideEntity
import com.example.domain.RideEvaluator
import com.example.domain.RideOffer
import com.example.domain.RideOfferParser
import com.example.domain.RidePlatform
import com.example.domain.SemaphoreVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarDomainTest {

    private val defaultSettings = DriverSettingsEntity(
        fuelPricePerLiter = 6.00,
        fuelConsumptionKmPerL = 10.0,
        maintenancePerKm = 0.25,
        dailyFixedCost = 40.0,
        targetRatePerKm = 2.00,
        minRatePerKm = 1.40,
        targetRatePerHour = 60.00,
        minRatePerHour = 40.00,
        dailyGoalNet = 250.0
    )

    @Test
    fun `parser extrai chamada da Uber com embarque em metros e viagem em km`() {
        val offer = RideOfferParser.parse(
            packageName = "com.ubercab.driver",
            lines = listOf(
                "UberX",
                "R$ 26,80",
                "4 min (800 m) de distância",
                "16 min (9,2 km) de viagem",
                "Aceitar"
            )
        )
        assertNotNull(offer)
        assertEquals(RidePlatform.UBER, offer!!.platform)
        assertEquals(26.80, offer.fare, 0.01)
        assertEquals(10.0, offer.totalKm, 0.01)
        assertEquals(20, offer.totalMinutes)
    }

    @Test
    fun `parser extrai chamada real da Uber com Est de R$ por km e ponto decimal`() {
        val offer = RideOfferParser.parse(
            packageName = "com.ubercab.driver",
            lines = listOf(
                "UberX",
                "Exclusivo",
                "R$ 9,58",
                "Est. de R$1,65/km",
                "4,98 (1318)",
                "Verificado",
                "4 min (1.0 km)",
                "R. Sd. Antônio de Oliveira, Inhoaíba, Rio de Janeiro",
                "16 minutos (4.8 km)",
                "Av. Cesário de Melo, 3752, Campo Grande, Rio de Janeiro",
                "Aceitar"
            )
        )
        assertNotNull(offer)
        assertEquals(RidePlatform.UBER, offer!!.platform)
        assertEquals(9.58, offer.fare, 0.01)
        assertEquals(5.8, offer.totalKm, 0.01)
        assertEquals(20, offer.totalMinutes)
    }

    @Test
    fun `parser extrai chamada da 99 corretamente`() {
        val offer = RideOfferParser.parse(
            packageName = "com.taxis99",
            lines = listOf(
                "99Pop",
                "R$ 18,50",
                "3 min • 1,1 km",
                "12 min • 6,4 km"
            )
        )
        assertNotNull(offer)
        assertEquals(RidePlatform.APP_99, offer!!.platform)
        assertEquals(18.50, offer.fare, 0.01)
        assertEquals(7.5, offer.totalKm, 0.01)
        assertEquals(15, offer.totalMinutes)
    }

    @Test
    fun `parser ignora tela sem distancia e tempo`() {
        val offer = RideOfferParser.parse(
            packageName = "com.ubercab.driver",
            lines = listOf("Saldo disponível", "R$ 120,00")
        )
        assertNull(offer)
    }

    @Test
    fun `avaliador classifica verde amarelo e vermelho corretamente`() {
        // Verde: 30 reais / 10 km = 3,00/km (>= 2,00) e 30 / 0,4h = 75/h (>= 60)
        val green = RideEvaluator.evaluate(
            RideOffer(RidePlatform.UBER, fare = 30.0, totalKm = 10.0, totalMinutes = 24),
            defaultSettings
        )
        assertEquals(SemaphoreVerdict.VERDE, green.verdict)
        assertEquals("Aceitar corrida, 30 reais", green.buildSpeechPhrase())
        // Combustível = 10/10 * 6 = 6.0; Manutenção = 10 * 0.25 = 2.5 -> Lucro = 21.5
        assertEquals(21.5, green.estimatedNetProfit, 0.01)

        // Amarelo: 22 reais / 10 km = 2,20/km (>= ideal) e 22 / 0,5h = 44/h (entre min 40 e ideal 60)
        val yellow = RideEvaluator.evaluate(
            RideOffer(RidePlatform.APP_99, fare = 22.0, totalKm = 10.0, totalMinutes = 30),
            defaultSettings
        )
        assertEquals(SemaphoreVerdict.AMARELO, yellow.verdict)
        assertEquals("Avaliar corrida, 22 reais", yellow.buildSpeechPhrase())

        // Vermelho: 12 reais / 10 km = 1,20/km (< 1,40) e 12 / 0,5h = 24/h (< 40)
        val red = RideEvaluator.evaluate(
            RideOffer(RidePlatform.UBER, fare = 12.50, totalKm = 10.0, totalMinutes = 30),
            defaultSettings
        )
        assertEquals(SemaphoreVerdict.VERMELHO, red.verdict)
        assertEquals("Recusar corrida, 12 reais e 50 centavos", red.buildSpeechPhrase())
    }

    @Test
    fun `calculo diario aplica odometro combustivel real pedagio manutencao e custo fixo`() {
        val rides = listOf(
            RideEntity(
                dayKey = "2026-11-25",
                platform = "Uber",
                fare = 120.0,
                totalKm = 40.0,
                totalMinutes = 120,
                ratePerKm = 3.0,
                ratePerHour = 60.0,
                estimatedNetProfit = 86.0,
                verdict = SemaphoreVerdict.VERDE.name
            ),
            RideEntity(
                dayKey = "2026-11-25",
                platform = "99",
                fare = 80.0,
                totalKm = 30.0,
                totalMinutes = 60,
                ratePerKm = 2.67,
                ratePerHour = 80.0,
                estimatedNetProfit = 54.5,
                verdict = SemaphoreVerdict.VERDE.name
            )
        )
        val expenses = listOf(
            ExpenseEntity(
                dayKey = "2026-11-25",
                type = ExpenseType.COMBUSTIVEL,
                description = "Posto",
                amount = 50.0,
                liters = 8.33
            ),
            ExpenseEntity(
                dayKey = "2026-11-25",
                type = ExpenseType.PEDAGIO,
                description = "Pedágio",
                amount = 10.0
            )
        )
        val shift = DailyShiftEntity(
            dayKey = "2026-11-25",
            odometerStartKm = 1000.0,
            odometerEndKm = 1080.0
        )

        val summary = RadarRepository.calculateSummary(
            dayKey = "2026-11-25",
            rides = rides,
            expenses = expenses,
            shift = shift,
            settings = defaultSettings
        )

        assertEquals(200.0, summary.grossTotal, 0.01)
        assertEquals(120.0, summary.grossUber, 0.01)
        assertEquals(80.0, summary.gross99, 0.01)
        assertEquals(80.0, summary.effectiveKm, 0.01)
        assertTrue(summary.fuelIsActual)
        assertEquals(50.0, summary.fuelCost, 0.01)
        // Manutenção: 80 km * 0.25 = 20.0
        assertEquals(20.0, summary.maintenanceCost, 0.01)
        assertEquals(10.0, summary.tollsAndExtrasCost, 0.01)
        assertEquals(40.0, summary.fixedDailyCost, 0.01)
        // Saldo líquido real: 200 - 50 - 20 - 10 - 40 = 80.0
        assertEquals(80.0, summary.netBalance, 0.01)
    }
}
