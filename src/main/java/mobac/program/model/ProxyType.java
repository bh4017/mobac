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
package mobac.program.model;

import mobac.utilities.I18nUtils;

public enum ProxyType {
	SYSTEM, // 
	APP_SETTINGS, //
	CUSTOM, //
	CUSTOM_W_AUTH;

	//private String text;

//	private ProxyType(String text) {
//		this.text = text;
//	}

	@Override
	public String toString() {
		switch(this)
		{
			case SYSTEM: return I18nUtils.localizedStringForKey("set_net_proxy_settings_java");
			case APP_SETTINGS: return I18nUtils.localizedStringForKey("set_net_proxy_settings_application");
			case CUSTOM: return I18nUtils.localizedStringForKey("set_net_proxy_settings_custom");
			case CUSTOM_W_AUTH: return I18nUtils.localizedStringForKey("set_net_proxy_settings_custom_auth");
		}
		return I18nUtils.localizedStringForKey("Undefined");
	}

}
