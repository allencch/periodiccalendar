/**
 *  Copyright (C) 2009-2010 by Mar'yan Rachynsky
 *  mrach@users.sourceforge.net
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.linuxorg.pcal.engine;

import net.sf.linuxorg.pcal.PCalDayInfo;

/**
 * This factory produces a common day info records
 * @author Mar'yan Rachynskyy
 *
 */
public class DayInfoRecordsFactory {
	/**
	 * Get the day info record for the recorded day 1 of either cycle or pregnancy
	 * @param rectype - the recorded day type
	 * @return the day information record
	 */
	public static PCalDayInfo getDay1Info(Integer rectype) {
		PCalDayInfo info = new PCalDayInfo();
		info.day_num = 0;
		info.fertile = false;
		info.pregnancy = (rectype == Engine.PCAL_TYPE_PREGNANCY);
		info.birth = (rectype == Engine.PCAL_TYPE_BIRTH);
		info.pregnancy_interruption = (rectype == Engine.PCAL_TYPE_PREGNANCY_INT);
		info.badFeel = true;
		info.estimate = false;
		info.ovulation = false;
		return info;
	}

	/**
	 * Returns the regular fertile record
	 * @param day_num - the day number to be assigned
	 * @return the day information record
	 */
	public static PCalDayInfo getFertileDayInfo(int day_num) {
		PCalDayInfo info = new PCalDayInfo();
		info.fertile = true;
		info.pregnancy = false;
		info.birth = false;
		info.pregnancy_interruption = false;
		info.badFeel = false;
		info.estimate = false;
		info.day_num = day_num;
		info.ovulation = false;
		return info;
	}

	/**
	 * Returns the regular non-fertile record
	 * @param day_num - the day number to be assigned
	 * @return the day information record
	 */
	public static PCalDayInfo getNonFertileDayInfo(int day_num) {
		PCalDayInfo info = new PCalDayInfo();
		info.fertile = false;
		info.pregnancy = false;
		info.birth = false;
		info.pregnancy_interruption = false;
		info.badFeel = false;
		info.estimate = false;
		info.day_num = day_num;
		info.ovulation = false;
		return info;
	}

	/**
	 * Returns the regular pregnancy day record
	 * @param day_num - the day number to be assigned
	 * @return the day information record
	 */
	public static PCalDayInfo getPregnancyDayInfo(int day_num) {
		PCalDayInfo info = new PCalDayInfo();
		info.fertile = false;
		info.pregnancy = true;
		info.birth = false;
		info.pregnancy_interruption = false;
		info.badFeel = true;
		info.estimate = false;
		info.day_num = day_num;
		info.ovulation = false;
		return info;
	}
	
	/**
	 * Returns the unknown date record. Only the day_num might have reasonable value
	 * @param day_num - the day number to be assigned
	 * @return the day information record
	 */
	public static PCalDayInfo getUnknownDayInfo(int day_num) {
		PCalDayInfo info = new PCalDayInfo();
		info.unknown = true;
		info.day_num = day_num;
		info.fertile = false;
		info.pregnancy = false;
		info.birth = false;
		info.pregnancy_interruption = false;
		info.badFeel = false;
		info.estimate = true;
		info.ovulation = false;
		return info;
	}
}
