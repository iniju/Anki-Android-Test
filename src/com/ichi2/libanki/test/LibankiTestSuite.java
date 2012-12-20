/****************************************************************************************
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/
package com.ichi2.libanki.test;

import android.test.suitebuilder.TestSuiteBuilder;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite containing all the libanki tests ported from python.
 *
 * To run from Eclipse, simply open this file in the editor and do Run as..., in the dialog that
 * appears select Android JUnit Test
 *
 * To run from command line, go to the platform-tools of the Android SDK and execute:
 * $ adb shell am instrument -w -e class com.ichi2.libanki.test.LibankiTestSuite \
 *   com.ichi2.anki.test/android.test.InstrumentationTestRunner
 */
public class LibankiTestSuite extends TestSuite {

    public static final Test suite() {
//    	TestSuite test = new TestSuite();
//    	test.addTest(new FindTestCase("test_findDupes"));
//    	return test;
    	return new TestSuiteBuilder(LibankiTestSuite.class).includeAllPackagesUnderHere().build();	
    }
}
