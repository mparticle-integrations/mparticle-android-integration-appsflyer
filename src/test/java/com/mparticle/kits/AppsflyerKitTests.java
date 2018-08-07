package com.mparticle.kits;


import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AppsflyerKitTests {

    private KitIntegration getKit() {
        return new AppsFlyerKit();
    }

    @Test
    public void testGetName() throws Exception {
        String name = getKit().getName();
        assertTrue(name != null && name.length() > 0);
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    public void testOnKitCreate() throws Exception{
        Exception e = null;
        try {
            KitIntegration kit = getKit();
            Map settings = new HashMap<>();
            settings.put("fake setting", "fake");
            kit.onKitCreate(settings, Mockito.mock(Context.class));
        }catch (Exception ex) {
            e = ex;
        }
        assertNotNull(e);
    }

    @Test
    public void testClassName() throws Exception {
        KitIntegrationFactory factory = new KitIntegrationFactory();
        Map<Integer, String> integrations = factory.getKnownIntegrations();
        String className = getKit().getClass().getName();
        for (Map.Entry<Integer, String> entry : integrations.entrySet()) {
            if (entry.getValue().equals(className)) {
                return;
            }
        }
        fail(className + " not found as a known integration.");
    }

    @Test
    public void testGenerateSkuString() throws Exception {
        MParticle.setInstance(Mockito.mock(MParticle.class));
        Mockito.when(MParticle.getInstance().getEnvironment()).thenReturn(MParticle.Environment.Production);
        assertNull(AppsFlyerKit.generateProductIdList(null));
        Product product = new Product.Builder("foo-name", "foo-sku", 50).build();
        CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, product)
                .transactionAttributes(new TransactionAttributes("foo"))
                .build();
        assertEquals("foo-sku", AppsFlyerKit.generateProductIdList(event));

        Product product2 = new Product.Builder("foo-name-2", "foo-sku-2", 50).build();
        CommerceEvent event2 = new CommerceEvent.Builder(Product.PURCHASE, product)
                .addProduct(product2)
                .transactionAttributes(new TransactionAttributes("foo"))
                .build();
        assertEquals("foo-sku,foo-sku-2", AppsFlyerKit.generateProductIdList(event2));

        Product product3 = new Product.Builder("foo-name-3", "foo-sku-,3", 50).build();
        CommerceEvent event3 = new CommerceEvent.Builder(Product.PURCHASE, product)
                .addProduct(product2)
                .addProduct(product3)
                .transactionAttributes(new TransactionAttributes("foo"))
                .build();
        assertEquals("foo-sku,foo-sku-2,foo-sku-%2C3", AppsFlyerKit.generateProductIdList(event3));
    }
}