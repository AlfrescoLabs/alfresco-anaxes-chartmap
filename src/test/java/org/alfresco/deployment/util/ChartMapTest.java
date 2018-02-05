package org.alfresco.deployment.util;

import org.junit.Test;
import org.junit.Assert;

    public class ChartMapTest {
        @Test
        public void getAlfrescoTestCharts() {
            Assert.assertEquals(0, ChartMap.getCharts("https://alfresco.github.io/charts-test/incubator"));
        }
        @Test
        public void getGoogleCloudCharts() {
            Assert.assertEquals(0, ChartMap.getCharts("https://kubernetes-charts-incubator.storage.googleapis.com"));
        }
    }