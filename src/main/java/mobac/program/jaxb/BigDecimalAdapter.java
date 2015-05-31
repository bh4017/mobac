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
package mobac.program.jaxb;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BigDecimalAdapter extends XmlAdapter<String, BigDecimal> {

	NumberFormat df = new DecimalFormat("0.00000",DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	@Override
	public BigDecimal unmarshal(String v) throws Exception {
		return new BigDecimal(v);
	}

	@Override
	public String marshal(BigDecimal v) throws Exception {
		return df.format(v);
	}
}
