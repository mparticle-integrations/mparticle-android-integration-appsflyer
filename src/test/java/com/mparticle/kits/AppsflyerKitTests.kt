package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito


class AppsflyerKitTests {

    private val kit = AppsFlyerKit()

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(name.isNotEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Throwable? = null
        try {
            val settings = HashMap<String,String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings as Map<String?, String?>, Mockito.mock(Context::class.java))
        } catch (ex: Throwable) {
            e = ex
        }
        Assert.assertNotNull(e)
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val factory = KitIntegrationFactory()
        val integrations = factory.knownIntegrations
        val className = kit.javaClass.name
        for (integration in integrations) {
            if (integration.value == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateSkuString() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()?.environment)
            .thenReturn(MParticle.Environment.Production)
        Assert.assertNull(AppsFlyerKit.generateProductIdList(null))
        val product = Product.Builder("foo-name", "foo-sku", 50.0).build()
        val event = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(TransactionAttributes("foo"))
            .build()
        assertEquals("foo-sku", AppsFlyerKit.generateProductIdList(event))
        val product2 = Product.Builder("foo-name-2", "foo-sku-2", 50.0).build()
        val event2 = CommerceEvent.Builder(Product.PURCHASE, product)
            .addProduct(product2)
            .transactionAttributes(TransactionAttributes("foo"))
            .build()
        assertEquals("foo-sku,foo-sku-2", AppsFlyerKit.generateProductIdList(event2))
        val product3 = Product.Builder("foo-name-3", "foo-sku-,3", 50.0).build()
        val event3 = CommerceEvent.Builder(Product.PURCHASE, product)
            .addProduct(product2)
            .addProduct(product3)
            .transactionAttributes(TransactionAttributes("foo"))
            .build()
        assertEquals("foo-sku,foo-sku-2,foo-sku-%2C3", AppsFlyerKit.generateProductIdList(event3))
    }
}