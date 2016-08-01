import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

import at.usmile.gaitmodule.utils.DataScaling;

/**
 * Created by Muhammad Muaaz on 10.08.2015.
 */
public class UtilTests extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @SmallTest
    public void testDataScaling(){
        double []data = new double[]{1.0,2.0,3.0,4.0,5.0};
        double [] testResult = new double[] {0.0, 0.25000,0.5000,0.75000, 1.0000} ;
        double[] results = DataScaling.scaledData(data,0,1);
        assertEquals(testResult,results);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }



}
