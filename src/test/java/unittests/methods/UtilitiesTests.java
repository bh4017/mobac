/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package unittests.methods;

import java.io.StringWriter;
import java.lang.reflect.Field;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import mobac.utilities.Utilities;
import mobac.utilities.tar.TarHeader;

public class UtilitiesTests extends TestCase {

	public void testParseSVNRevision() {
		assertEquals(4168, Utilities.parseSVNRevision("4168"));
		assertEquals(4168, Utilities.parseSVNRevision("4123:4168"));
		assertEquals(4168, Utilities.parseSVNRevision("4168M"));
		assertEquals(4168, Utilities.parseSVNRevision("4212:4168MS"));
		assertEquals(4168, Utilities.parseSVNRevision("$Revision:	4168$"));
		assertEquals(4168, Utilities.parseSVNRevision("$Rev: 4212:4168MS$"));
		assertEquals(-1, Utilities.parseSVNRevision("exported"));
	}

	public void testTarHeader() throws Exception {

		final String str1 = "Test123";
		final String str2 = "abcdefghijklmnopqrstuvwxyz";
		final String str3 = "1234567890";
		StringWriter sw = new StringWriter(110);
		for (int i = 0; i < 100; i++)
			sw.write('x');
		String str4 = sw.toString();

		for (int i = 0; i < 101; i++)
			sw.write('y');
		String str5 = sw.toString();

		TarHeader tarHeader = new TarHeader(str1, 12345, false);
		assertEquals(str1, tarHeader.getFileName());

		tarHeader.setFileName(str2);
		assertEquals(str2, tarHeader.getFileName());

		tarHeader.setFileName(str3);
		assertEquals(str3, tarHeader.getFileName());

		tarHeader.setFileName(str4); // max length filename
		assertEquals(str4, tarHeader.getFileName());

		try {
			tarHeader.setFileName(str5);
			fail("Exception expected");
		} catch (Exception e) {
			
		}

		Field f = TarHeader.class.getDeclaredField("fileName");
		f.setAccessible(true);
		char[] chars = (char[]) f.get(tarHeader);
		for (char c : chars)
			System.out.print(c == 0 ? " 0" : c);
	}

	public static void main(String[] args) {
		TestRunner.run(UtilitiesTests.class);
	}

}
