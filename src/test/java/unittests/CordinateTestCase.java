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
package unittests;

import junit.framework.TestCase;
import mobac.utilities.Utilities;
import mobac.utilities.geo.CoordinateDm2Format;
import mobac.utilities.geo.CoordinateDms2Format;

public class CordinateTestCase extends TestCase {

	public CordinateTestCase() {
		super();
	}

	public void testCoordinateDm2Format() throws Exception {
		CoordinateDm2Format cf = new CoordinateDm2Format(Utilities.DFS_ENG);

		assertEquals("03° 30.00'", cf.format(3.5d));
		assertEquals("-03° 30.00'", cf.format(-3.5d));

		assertEquals("-20° 06.00'", cf.format(-20.1d));
		assertEquals("20° 06.00'", cf.format(20.1d));

		assertEquals("-13° 54.00'", cf.format(-13.9d));
		assertEquals("13° 54.00'", cf.format(13.9d));

		assertEquals("00° 06.00'", cf.format(0.1d));
		assertEquals("-00° 06.00'", cf.format(-0.1d));
		
		assertEquals(-4.25, cf.parse("-04° 15.0'"));
		assertEquals(+4.25, cf.parse("+04° 15.0'"));

		assertEquals(-20.1, cf.parse("-20° 6'"));
		assertEquals(+20.1, cf.parse("+20° 6'"));

		assertEquals(-13.9, cf.parse("-13° 54'"));
		assertEquals(+13.9, cf.parse("+13° 54'"));

		assertEquals(-0.1, cf.parse("-00° 06'"));
		assertEquals(+0.1, cf.parse("+00° 06'"));

	}

	public void testCoordinateDms2Format() throws Exception {
		CoordinateDms2Format cf = new CoordinateDms2Format(Utilities.DFS_ENG);

		assertEquals("03° 32' 59.99\"", cf.format(3.55d));
		assertEquals("-03° 32' 59.99\"", cf.format(-3.55d));

		assertEquals("-20° 06' 35.99\"", cf.format(-20.11d));
		assertEquals("20° 06' 35.99\"", cf.format(20.11d));

		assertEquals("-13° 59' 24.00\"", cf.format(-13.99d));
		assertEquals("13° 59' 24.00\"", cf.format(13.99d));

		assertEquals("00° 06' 00.00\"", cf.format(0.1d));
		assertEquals("-00° 06' 00.00\"", cf.format(-0.1d));

		assertEquals(-3.55, cf.parse("-03° 32' 60.00\""));
		assertEquals(+3.55, cf.parse("+03° 32' 60.00\""));

		assertEquals(-2011, (int) (cf.parse("-20° 6' 36\"").doubleValue() * 100d));
		assertEquals(+2011, (int) (cf.parse("+20° 6' 36\"").doubleValue() * 100d));

		assertEquals(-1390, (int) (cf.parse("-13° 54' 24\"").doubleValue() * 100));
		assertEquals(+1390, (int) (cf.parse("+13° 54' 24\"").doubleValue() * 100));

		assertEquals(-0.1, cf.parse("-00° 06' 0\""));
		assertEquals(+0.1, cf.parse("+00° 06' 0\""));
}
}
