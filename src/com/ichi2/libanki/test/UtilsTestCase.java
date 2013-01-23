package com.ichi2.libanki.test;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import com.ichi2.libanki.Utils;
import org.json.JSONException;
import org.json.JSONObject;

public class UtilsTestCase extends InstrumentationTestCase {
    @SmallTest
    public void testJSONEscapes() throws JSONException {
        JSONObject jo1 = new JSONObject("{'mykey': '<span>aaa</span>'}");
        String serialization1 = Utils.jsonDumps(jo1);
        assertEquals(serialization1, "{\"mykey\":\"<span>aaa</span>\"}");
        JSONObject jo2 = new JSONObject("{'mykey': '<span>aaa<\\/span>'}");
        String serialization2 = Utils.jsonDumps(jo2);
        assertEquals(serialization2, serialization1);
    }
}
