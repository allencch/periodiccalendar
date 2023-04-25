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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.linuxorg.pcal.PCalDayInfo;
import net.sf.linuxorg.pcal.PCalPeriodInfo;
import net.sf.linuxorg.pcal.PCalendar;
import net.sf.linuxorg.pcal.messages.Messages;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// import com.sun.org.apache.xml.internal.serialize.OutputFormat;
// import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * @author Mar'yan Rachynskyy
 *
 */

public class Engine {

	public class PasswordRequiredException extends Exception {
		private static final long serialVersionUID = 1L;
	};

	private EnginePreferences enginePreferences = new EnginePreferences();

	private static final String FILE_VERSION = "2.0"; //$NON-NLS-1$
	private static final int FILE_TYPE_PLAIN = 0;
	private static final int FILE_TYPE_ENCRYPTED = 1;
	private static final int FILE_TYPE_UNKNOWN = -1;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd"); //$NON-NLS-1$
	// Date record type ID's
	protected static final Integer PCAL_TYPE_MENS = 1;
	protected static final Integer PCAL_TYPE_PREGNANCY = 2;
	protected static final Integer PCAL_TYPE_BIRTH = 3;
	protected static final Integer PCAL_TYPE_PREGNANCY_INT = 4;
	protected static final Integer PCAL_TYPE_OVULATION = 5;

	// Date-value pairs type ID's
	private static final Integer PCAL_TYPE_NOTES = 1;
	private static final Integer PCAL_TYPE_BBT = 2;
	private static final Integer PCAL_TYPE_DATE_INTS = 3;

	private static final int DEFAULT_BUFFER_DAYS = 0;
	private static final int DEFAULT_BAD_FEEL_DAYS_BEFORE = 0;
	private static final int DEFAULT_BAD_FEEL_DAYS_AFTER = 0;
	private static final int DEFAULT_REGULAR_MIN_LENGTH = 24;
	private static final int DEFAULT_REGULAR_MAX_LENGTH = 35;

	private static final String BBT_VALUE = "BBT_VALUE"; //$NON-NLS-1$
	private static final String BBT_SYMPTHOM = "BBT_SYMPTHOM"; //$NON-NLS-1$

	// These are the engine prediction constants which are used during the calculations
	private static final int REGULAR_PREGNANCY_DAYS = 280; /* 40 weeks * 7 */
	private static final int MAX_PREGNANCY_DAYS = 308; /* 44 weeks * 7 */
	private static final int RELIABLE_LENGTH_DIFF = 2;
	private static final int RELIABLE_MAX_AVG_LENGTH = 33;
	private static final int RELIABLE_MIN_AVG_LENGTH = 25;
	private static final int RELIABLE_PERIODS_COUNT = 5;
	private static final int UNRELIABLE_MAX_AVG_LENGTH = 35;
	private static final int UNRELIABLE_MIN_AVG_LENGTH = 24;
	private static final int UNRELIABLE_LENGTH_DIFF = 3;
	private static final int PERIOD_FERTILE_DAYS_COUNT = 6;
	private static final int PERIOD_END_NON_FERTILE_DAYS_COUNT = 11;

	private static final String FILE_PROPERTIES_TAG = "FileProperties"; //$NON-NLS-1$

	private static final String CELSIUS_SCALE = "CELSIUS_SCALE"; //$NON-NLS-1$
	private static final String SCALE_TEMP_MAX = "SCALE_TEMP_MAX"; //$NON-NLS-1$
	private static final String SCALE_TEMP_MIN = "SCALE_TEMP_MIN"; //$NON-NLS-1$
	private static final String TRUE = "True"; 					//$NON-NLS-1$
	private static final String FALSE = "False";				//$NON-NLS-1$
    public static final int DEFAULT_BBT_TEMPERATURE_MIN_C = 360; // default minimum temperature
    public static final int DEFAULT_BBT_TEMPERATURE_MIN_F = 968;

	private TreeSet<Date> startDates = new TreeSet<Date>(); /// the calendar events index
	private HashMap<Date, Integer> dateTypes = new HashMap<Date, Integer>(); /// the types of the calendar events listed in start_dates
	private HashMap<Date, String> notesContainer = new HashMap<Date, String>(); /// the notes container
	private HashMap<Date, Integer> bBTList = new HashMap<Date, Integer>(); /// the BBT records list
	private boolean fileBBTCelsiusScale = true; // if true - the file contains BBT records in the Celsius scale, otherwise - in Fahrenheit
	private static final int DEFAULT_BBT_TEMPERATURE_LEVELS_C = 16;
	private static final int DEFAULT_BBT_TEMPERATURE_LEVELS_F = 29;
	private int minTempScale = DEFAULT_BBT_TEMPERATURE_MIN_C; // minimum temperature on a graph scale
	private int maxTempScale = DEFAULT_BBT_TEMPERATURE_MIN_C + DEFAULT_BBT_TEMPERATURE_LEVELS_C-1; // maximum temperature on a graph scale
	private HashMap<Date, DateIntsContainer> dateIntValues = new HashMap<Date, DateIntsContainer>(); /// the date-value container for the integer values
	private BBTSympthomsSetDefinition sympthomsDefinition = new BBTSympthomsSetDefinition(); // the container for the additional sympthoms list to be monitored
	private HashMap<Integer, String> dateTypeToTagMap = new HashMap<Integer, String>(); /// map the date type code to the tag name
	private HashMap<Integer, String> dateValueTypeToTagMap = new HashMap<Integer, String>(); /// map the date-value type code to the tag name

	private boolean modified = false; /// true if any changes were done since last save or load the statistics data
	private int periodsCount = 0; /// regular periods count
	private int avgLength = 0;
	private int minLength = 0;
	private int maxLength = 0;
	private int minOvulationDayNum = 0;
	private int maxOvulationDayNum = 0;
	private int recordedOvulations = 0;
	private int calMethodAccuracy = 3; /// 0 - high, 1 - moderate, 2 - low, 3 - no data

	private File workFile = null;
	private SecretKey workFileKey = null;

	public Engine() {

		// initialize the date types map
		dateTypeToTagMap.put(PCAL_TYPE_MENS, "mens"); //$NON-NLS-1$
		dateTypeToTagMap.put(PCAL_TYPE_PREGNANCY, "pregnancy"); //$NON-NLS-1$
		dateTypeToTagMap.put(PCAL_TYPE_BIRTH, "birth");  //$NON-NLS-1$
		dateTypeToTagMap.put(PCAL_TYPE_PREGNANCY_INT, "pregnancyint");  //$NON-NLS-1$
		dateTypeToTagMap.put(PCAL_TYPE_OVULATION, "ovulation");  //$NON-NLS-1$

		// initialize the date-value pair types map
		dateValueTypeToTagMap.put(PCAL_TYPE_NOTES, "note"); //$NON-NLS-1$
		dateValueTypeToTagMap.put(PCAL_TYPE_BBT, "bbt"); //$NON-NLS-1$
		dateValueTypeToTagMap.put(PCAL_TYPE_DATE_INTS, "dateints"); //$NON-NLS-1$

		Preferences enginePrefsNode = PCalendar.settings.node("Engine"); //$NON-NLS-1$
		enginePreferences.bufferDays = enginePrefsNode.getInt("Buffer Days", DEFAULT_BUFFER_DAYS); //$NON-NLS-1$
		enginePreferences.badFeelDaysBefore = enginePrefsNode.getInt("Bad Feels Before", DEFAULT_BAD_FEEL_DAYS_BEFORE); //$NON-NLS-1$
		enginePreferences.badFeelDaysAfter = enginePrefsNode.getInt("Bad Feels After", DEFAULT_BAD_FEEL_DAYS_AFTER); //$NON-NLS-1$
		enginePreferences.regularMinLength = enginePrefsNode.getInt("Reg. Min Length", DEFAULT_REGULAR_MIN_LENGTH); //$NON-NLS-1$
		enginePreferences.regularMaxLength = enginePrefsNode.getInt("Reg. Max Length", DEFAULT_REGULAR_MAX_LENGTH); //$NON-NLS-1$
		newFile();
		recalculate();

		//listen to the sympthom list changes
		sympthomsDefinition.addModifiedListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				modified = true;
			}
		});
	}

	/**
	 * This is the main method of the entire application. The calendar day color and properties are determined here
	 * @param date - the date being analyzed
	 * @return date information or NULL if no data available
	 */
	public PCalDayInfo getDayInfo(final Date date)
	{
		if(startDates.isEmpty()) {
			// no data at all
			return null;
		} else {
			PCalDayInfo info = null;

			Integer rectype = dateTypes.get(date);
			if(rectype != null) {
				if(rectype == PCAL_TYPE_OVULATION) {
					// ovulation is processed in other way
					info = getOvulationDayInfo(date);
					info.ovulation = true;
				} else {
					// this date is really in the database as day 1 (mens, pregnancy or birth)
					info = DayInfoRecordsFactory.getDay1Info(rectype);
				}
			}
			else
				if(date.before(startDates.first()))	{
					// the requested date is before the recorded periods
					return null;
				} else {
					Date last_date = startDates.last();
					// is the date in the recorded period?
					if(date.before(last_date)) {
						info = getDayInfoRecorded(date);
					} else {
						info = getDayInfoEstimated(date, last_date);
					}
				}
			return info;
		}
	}

	/**
	 * This inner method evaluated the date information for the dates beyond the recorded periods
	 * @param date - the date being analyzed
	 * @param last_date - the last recorded date
	 * @return the day info record or NULL if no data available
	 */
	private PCalDayInfo getDayInfoEstimated(final Date date, Date last_date) {
		PCalDayInfo info;

		// the day parameters should be predicted
		int day_num = dateDiff(last_date, date);
		int last_date_type = dateTypes.get(last_date);

		if(last_date_type == PCAL_TYPE_OVULATION) {

			int daysSinceOvulation = day_num;

			Date periodStartDate = getPeriodStartDate(last_date, true);
			if(periodStartDate == null) {
				// can not determine the day #
				day_num = -1;
			} else {
				day_num = dateDiff(periodStartDate, date);
			}

			if(daysSinceOvulation <= PERIOD_END_NON_FERTILE_DAYS_COUNT) {
				// this is definitely non-fertile regular date
				info = DayInfoRecordsFactory.getNonFertileDayInfo(day_num);
				info.badFeel = (PERIOD_END_NON_FERTILE_DAYS_COUNT - daysSinceOvulation) < enginePreferences.badFeelDaysBefore;
				return info;
			}

			// On 12th day the regular cycles are expected to restart
			if(daysSinceOvulation == (PERIOD_END_NON_FERTILE_DAYS_COUNT + 1)) {
				// this is definitely a mens day
				info = DayInfoRecordsFactory.getDay1Info(PCAL_TYPE_MENS);
				info.estimate = true;
				return info;
			}

			// Being here means we need to restart a regular cycles estimation basing on the first cycle which follows
			// the ovulation
			last_date_type = PCAL_TYPE_MENS;
			day_num = daysSinceOvulation - PERIOD_END_NON_FERTILE_DAYS_COUNT - 1;
		}

		if(last_date_type == PCAL_TYPE_MENS) {
			if(periodsCount == 0) {
				// There is nothing we can say about this day except the number of days since the last mens
				if(day_num > enginePreferences.regularMaxLength) {
					// too far in the future
					return null;
				} else {
					info = DayInfoRecordsFactory.getUnknownDayInfo(day_num);
					return info;
				}
			}

			// the cycle number is calculated by the average cycle length
			if(avgLength!=0)
				day_num %= avgLength;

			if(day_num == 0) {
				// most probably this will be a day 1
				info = DayInfoRecordsFactory.getDay1Info(PCAL_TYPE_MENS);
			} else {
				info = DayInfoRecordsFactory.getNonFertileDayInfo(day_num);
				// this is a day after the last recorded period start
				if(recordedOvulations >12 ) {
					// use the average ovulation date as a basis to predict fertility
					info.fertile = (info.day_num >= (minOvulationDayNum - 9 - enginePreferences.bufferDays))
							&& (info.day_num <= (maxOvulationDayNum + enginePreferences.bufferDays));
				} else {
					// the number of non-fertile days at the beginning of the cycle is
					// considered as the minimum cycle length - 17 (PERIOD_FERTILE_DAYS_COUNT + PERIOD_END_NON_FERTILE_DAYS_COUNT)
					info.fertile = (info.day_num >= (minLength - PERIOD_FERTILE_DAYS_COUNT - PERIOD_END_NON_FERTILE_DAYS_COUNT - enginePreferences.bufferDays))
							&& (info.day_num < (maxLength - PERIOD_END_NON_FERTILE_DAYS_COUNT + enginePreferences.bufferDays));
				}
				info.badFeel = (info.day_num<=enginePreferences.badFeelDaysAfter)||
						((avgLength-info.day_num)<=enginePreferences.badFeelDaysBefore);
			}
		} else
			if(last_date_type == PCAL_TYPE_PREGNANCY) {
				if(day_num < REGULAR_PREGNANCY_DAYS) {
					info = DayInfoRecordsFactory.getPregnancyDayInfo(day_num);
				} else
					if(day_num == REGULAR_PREGNANCY_DAYS) {
						info = DayInfoRecordsFactory.getDay1Info(PCAL_TYPE_BIRTH);
					} else {
						// this is a day after the normal pregnancy period
						// know nothing
						info = DayInfoRecordsFactory.getNonFertileDayInfo(-1);
					}
			} else {
				// know nothing
				info = DayInfoRecordsFactory.getNonFertileDayInfo(-1);
			}
		info.estimate = true;
		return info;
	}


	/**
	 * This inner method evaluated the date information for the dates within recorded periods
	 * @param date - the date being analyzed
	 * @return the day info record or NULL if no data available
	 */
	private PCalDayInfo getDayInfoRecorded(final Date date) {
		PCalDayInfo info = null;

		// find the starting date of the date's period
		Date periodStartDate = getPeriodStartDate(date, true);
		if(periodStartDate!=null) {
			// this is a date in the complete period
			// this is not a day 1 - we have already checked this
			int date_type = dateTypes.get(periodStartDate);
			int day_num = dateDiff(date, periodStartDate);

			if(date_type == PCAL_TYPE_MENS) {
				Date periodEndDate = getMensPeriodEndDate(periodStartDate);
				Date periodOvulationDate = getMensPeriodOvulationDate(periodStartDate);
				boolean fertileDay = false;
				int period_length = 0;
				int daysTillEnd = 0;

				if(periodOvulationDate == null) {
					// ovulation is not recorded. Let's check for the end date
					if(periodEndDate == null) {
						// if periodEndDate is null
						// the period is incorrect
						// no information can be provided on it
						return null;
					} else {
						period_length = dateDiff(periodStartDate, periodEndDate);
						// zero means we have no periods statistics
						daysTillEnd = (period_length==0)?0:(period_length - day_num);

						if(daysTillEnd==0) {
							// better to say unknown here... this case normally should never happen
							fertileDay  = false;
						} else {
							fertileDay = (daysTillEnd < 18) && (daysTillEnd > PERIOD_END_NON_FERTILE_DAYS_COUNT);
						}
					}
				} else {
					if(periodEndDate != null) {
						// daysTillEnd still needed here to have a chance to estimate bad feelings
						period_length = dateDiff(periodStartDate, periodEndDate);
						// zero means we have no periods statistics
						daysTillEnd = (period_length==0)?0:(period_length - day_num);
					}
					if(date.before(periodOvulationDate)) {
						// the date is between the cycle start and the ovulation date
						int daysTillOvulationEnd = dateDiff(date, periodOvulationDate);
						fertileDay = daysTillOvulationEnd < PERIOD_FERTILE_DAYS_COUNT;
					} else {
						// This is definitely non-fertile date
						fertileDay = false;
					}
				}

				if(fertileDay) {
					info = DayInfoRecordsFactory.getFertileDayInfo(day_num);
				} else {
					info = DayInfoRecordsFactory.getNonFertileDayInfo(day_num);
				}
				if(daysTillEnd == 0) {
					info.badFeel = (day_num<=enginePreferences.badFeelDaysAfter);
				} else {
					info.badFeel = (day_num<=enginePreferences.badFeelDaysAfter)||(daysTillEnd<=enginePreferences.badFeelDaysBefore);
				}

			} else
				if(date_type == PCAL_TYPE_PREGNANCY) {
					Date nextDate = startDates.higher(periodStartDate);
					int nextDateType = dateTypes.get(nextDate);
					if(((nextDateType == PCAL_TYPE_BIRTH) ||
							(nextDateType == PCAL_TYPE_PREGNANCY_INT))
							&& (day_num <= MAX_PREGNANCY_DAYS)) {
						// the pregnancy maybe took longer but still completion was recorded
						info = DayInfoRecordsFactory.getPregnancyDayInfo(day_num);
					} else
						if(day_num <= REGULAR_PREGNANCY_DAYS) {
							//pregnancy completion was not recorded - actually this is the prediction
							info = DayInfoRecordsFactory.getPregnancyDayInfo(day_num);
						} else {
							// this is a day after the normal pregnancy period
							// know nothing
							info = DayInfoRecordsFactory.getNonFertileDayInfo(-1);
						}
				} else {
					// know nothing
					info = DayInfoRecordsFactory.getNonFertileDayInfo(-1);
				}
		} else {
			// we still can say something if the date is followed by the ovulation date
			Boolean ovulationFound = checkForFutureOvulation(date);

			if(ovulationFound == null) {
				// know nothing
				info = null;
			} else
				if(ovulationFound) {
					// at least we know this is a fertile day
					info = DayInfoRecordsFactory.getFertileDayInfo(-1);
				}
		}
		return info;
	}

	/**
	 * Get the day info record for the recorded ovulation date
	 * @param date - the record date
	 * @return the day information record
	 */
	private PCalDayInfo getOvulationDayInfo(final Date date) {
		final int MIN_CYCLE_DAYS_AFTER_OVULATION = 14;

		PCalDayInfo info = DayInfoRecordsFactory.getFertileDayInfo(-1);

		// calculating the period day number is the only non-trivial step here
		Date period_start_date = getPeriodStartDate(date, true);
		if(dateTypes.get(period_start_date) == PCAL_TYPE_MENS) {
			info.day_num = dateDiff(date, period_start_date);

			// the ovulation can no be at the very end of the cycle
			if(info.day_num > (enginePreferences.regularMaxLength - MIN_CYCLE_DAYS_AFTER_OVULATION)) {
				// we are out of the regular period, don't know the period day #
				info.day_num = -1;
			}
		}
		return info;
	}

	/**
	 * Find the date's period start date. Returns null if no adequate date was found.
	 * This method can return the menstrual period start or the pregnancy start.
	 * This method does not check if the menstrual period is not shorter
	 * than the minimum allowed.
	 * @param date - a date within the period
	 * @param checkMensLength - if true, the menstrual period length boundaries are checked. False is useful for the future predictions, when the date can be beyond the length of a single cycle
	 * @return - the day info record
	 */
	private Date getPeriodStartDate(final Date date, final boolean checkMensLength) {
		Date period_start_date = null;

		if(date == null) return period_start_date;

		Date date_pointer = startDates.lower(date);

		while(date_pointer != null) {
			int pointer_date_type = dateTypes.get(date_pointer);

			if((pointer_date_type == PCAL_TYPE_BIRTH) ||
					(pointer_date_type == PCAL_TYPE_PREGNANCY_INT)) {
				// the last period has ended, the new period was not started
				break;
			} else
				if(pointer_date_type == PCAL_TYPE_MENS) {
					int day_number = dateDiff(date_pointer, date);
					if(checkMensLength) {
						if(day_number < enginePreferences.regularMaxLength) {
							period_start_date = date_pointer;
						}
					} else {
						period_start_date = date_pointer;
					}
					break;
				}
			if(pointer_date_type == PCAL_TYPE_PREGNANCY) {
				int day_number = dateDiff(date_pointer, date);
				if(checkMensLength) {
					if(day_number < MAX_PREGNANCY_DAYS) {
						period_start_date = date_pointer;
					}
				} else {
					period_start_date = date_pointer;
				}
				break;
			}
			else {
				// lets look for the earlier recorded date
				date_pointer = startDates.lower(date_pointer);
			}
		}
		return period_start_date;
	}

	/**
	 * This method locates the menstrual period end date
	 * @param periodStartDate - period start date
	 * @return end date or null if periodStartDate does not start a valid period
	 */
	private Date getMensPeriodEndDate(final Date periodStartDate) {
		Date periodEndDate = null;

		if(periodStartDate == null) return periodEndDate;
		if(dateTypes.get(periodStartDate) == null) return periodEndDate;

		Date date_pointer = startDates.higher(periodStartDate);

		while(date_pointer != null) {
			int pointer_date_type = dateTypes.get(date_pointer);

			if((pointer_date_type == PCAL_TYPE_BIRTH) ||
					(pointer_date_type == PCAL_TYPE_PREGNANCY_INT)) {
				// the last period has ended, the new period was not started
				break;
			} else
				if((pointer_date_type == PCAL_TYPE_MENS) ||
						(pointer_date_type == PCAL_TYPE_PREGNANCY)) {
					int periodLength = dateDiff(periodStartDate, date_pointer);
					if((periodLength >= enginePreferences.regularMinLength) &&
							(periodLength <= enginePreferences.regularMaxLength)) {
						periodEndDate = date_pointer;
					}
					break;
				}
				else {
					// lets look for the earlier recorded date
					date_pointer = startDates.higher(date_pointer);
				}
		}
		return periodEndDate;
	}

	/**
	 * This method locates the menstrual period ovulation date
	 * @param periodStartDate - period start date
	 * @return ovulation date or null if periodStartDate does not contain an ovulation date
	 */
	private Date getMensPeriodOvulationDate(final Date periodStartDate) {
		Date periodOvulationDate = null;

		if(periodStartDate == null) return periodOvulationDate;
		if(dateTypes.get(periodStartDate) == null) return periodOvulationDate;

		periodOvulationDate = startDates.higher(periodStartDate);

		if(periodOvulationDate != null) {
			if(dateTypes.get(periodOvulationDate) == PCAL_TYPE_OVULATION) {
				int ovulationDayNumber = dateDiff(periodStartDate, periodOvulationDate);
				if(ovulationDayNumber>(enginePreferences.regularMaxLength)) {
					// this ovulation is beyond this period
					periodOvulationDate = null;
				}
			} else {
				periodOvulationDate = null;
			}
		}
		return periodOvulationDate;
	}

	/**
	 * Checks if after the date specified, there is an ovulation record.
	 * @param date - the date under investigation
	 * @return - null if there is no ovulation recorded after the date specified
	 * 			 True - if the ovulation record is present and the date is fertile
	 *           False - if the ovulation record is present can not cause this date fertility
	 */
	private Boolean checkForFutureOvulation(Date date) {
		Date next_date = startDates.higher(date);
		int next_date_type = dateTypes.get(next_date);
		if(next_date_type == PCAL_TYPE_OVULATION) {
			int days_till_ovulation = dateDiff(date, next_date);
			return days_till_ovulation < PERIOD_FERTILE_DAYS_COUNT;
		} else {
			return null;
		}
	}

	/**
	 * Load the data from file. If the file was opened with a password, Engine will store
	 * the password internally so transparent "Save" operation will be performed without
	 * re-requesting the password entry.
	 * @param file - a file to be loaded
	 * @param password - a password to be used for the file.
	 * If the file appears to be unencrypted, this parameter is ignored.
	 * @throws SAXException - file content format is unparsable
	 * @throws IOException - can not read a file specified
	 * @throws ParserConfigurationException - can not set up the file parser - internal error
	 * @throws PasswordRequiredException is thrown if password parameter is empty but the file is encrypted
	 */
	public void loadFromFile(final File file, char[] password)
			throws SAXException, IOException, ParserConfigurationException, PasswordRequiredException
			{
		SecretKey secretKey = null;

		FileInputStream fileStream = new FileInputStream(file);

		GZIPInputStream f = new GZIPInputStream(fileStream);

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);

		int docType = getDocumentType(doc);

		switch(docType) {
		case FILE_TYPE_UNKNOWN:
			throw new SAXException(Messages.getString("Engine.0")); //$NON-NLS-1$
		case FILE_TYPE_ENCRYPTED: {
			if((password == null) || (password.length==0)) {
				throw new PasswordRequiredException();
			}
			secretKey = EngineXMLCrypter.generateSecretKey(password);
			doc = EngineXMLCrypter.decryptXML(doc, secretKey);

			//check if the encryption container contains a regular plain file
			int internalDocType = getDocumentType(doc);
			if(internalDocType != FILE_TYPE_PLAIN) {
				throw new SAXException(Messages.getString("Engine.0")); //$NON-NLS-1$
			}
		}
		case FILE_TYPE_PLAIN:
			// actually - nothing to do here. The file is ready for parsing.
		}

		//reinitialize the containers
		clear();

		//load the date types (mens, pregnancy, birh)
		loadFromFileDates(doc);

		//load the dates with the associated values (notes, BBT values, integers)
		loadFromFileDateValues(doc);

		//load the file properties
		loadFromFileProperties(doc);

		// store the password if applicable
		if(docType == FILE_TYPE_ENCRYPTED) {
			workFileKey = secretKey;
		}

		setWorkFile(file);
		modified = false;
		recalculate();
			}

	/**
	 * loadFromFile helper
	 * @param doc
	 * @throws SAXException
	 */
	private void loadFromFileDates(final Document doc) throws SAXException {
		for(Integer typeID: dateTypeToTagMap.keySet()) {
			NodeList nl;
			String text;
			Date date;
			nl = doc.getElementsByTagName(dateTypeToTagMap.get(typeID));
			for(int i = 0; i<nl.getLength();i++) {
				text = nl.item(i).getTextContent();
				if(text == null) {
					throw new SAXException(Messages.getString("Engine.1")); //$NON-NLS-1$
				}
				try {
					date = DATE_FORMAT.parse(text);
				} catch (ParseException e) {
					throw new SAXException(Messages.getString("Engine.2")+text); //$NON-NLS-1$
				}
				startDates.add(date);
				dateTypes.put(date, typeID);
			}
		}
	}

	/**
	 * loadFromFile helper
	 * @param doc
	 * @throws SAXException
	 */
	private void loadFromFileDateValues(final Document doc) throws SAXException {
		for(Integer valueTypeID: dateValueTypeToTagMap.keySet()) {
			NodeList nl;
			Date date;
			String text;
			nl = doc.getElementsByTagName(dateValueTypeToTagMap.get(valueTypeID));
			for(int i = 0; i<nl.getLength();i++) {
				NodeList nmp = nl.item(i).getChildNodes();
				date = null;
				text = null;
				for(int j=0;j<nmp.getLength();j++) {
					Node tmpNode = nmp.item(j);
					if(tmpNode.getNodeName().equals("date")) { //$NON-NLS-1$
						String tmpText = tmpNode.getTextContent();
						if(tmpText == null) {
							throw new SAXException(Messages.getString("Engine.1")); //$NON-NLS-1$
						}
						try {
							date = DATE_FORMAT.parse(tmpText);
						} catch (ParseException e) {
							throw new SAXException(Messages.getString("Engine.2")+text); //$NON-NLS-1$
						}

					} else
						if(tmpNode.getNodeName().equals("text")) { //$NON-NLS-1$
							text = tmpNode.getTextContent();
						}
				}
				if((text == null) || (date == null)) {
					throw new SAXException(Messages.getString("Engine.1")); //$NON-NLS-1$
				}

				if(valueTypeID.equals(PCAL_TYPE_NOTES)) {
					notesContainer.put(date, text);
				} else
					if(valueTypeID.equals(PCAL_TYPE_BBT)) {
						try {
							bBTList.put(date, Integer.parseInt(text));
						}
						catch (NumberFormatException e) {
							// Even the minor file corruption can be a big problem. Stop everything.
							// Incorrect BBT entry
							throw new SAXException(Messages.getString("Engine.4")); //$NON-NLS-1$
						}
					} else
						if(valueTypeID.equals(PCAL_TYPE_DATE_INTS)) {
							try {
								dateIntValues.put(date, DateIntsContainer.parseFromStringFactory(text));
							}
							catch (NumberFormatException e) {
								// Even the minor file corruption can be a big problem. Stop everything.
								// Incorrect Date Integer Entry
								throw new SAXException(Messages.getString("Engine.3")); //$NON-NLS-1$
							}
						}

			}
		}
	}

	/**
	 * Load from file helper. Loads the properties associated with the file
	 * @param doc
	 * @throws SAXException
	 */
	private void loadFromFileProperties(Document doc) throws SAXException {
		Properties fileProperties = null;
		NodeList nl;
		nl = doc.getElementsByTagName(FILE_PROPERTIES_TAG);
		if(nl.getLength()>0) {
			if(nl.getLength()>1) {
				// something wrong - too many properties sections
				throw new SAXException(Messages.getString("Engine.0")); //$NON-NLS-1$
			}
			String text = nl.item(0).getTextContent();
			fileProperties = new Properties();

			// all non-ascii chars are translated automatically to the escape sequences
			// it should be safe to use direct bytes transformation here
			ByteArrayInputStream bai = new ByteArrayInputStream(text.getBytes());
			try {
				fileProperties.load(bai);
			} catch (IOException e) {
				throw new SAXException(Messages.getString("Engine.0")); //$NON-NLS-1$
			}
			parseLoadedFileProperties(fileProperties);
		}
	}

	/**
	 * Process the file properties and fill the internal structures with their values
	 * @param fileProperties the file properties to be processed
	 */
	private void parseLoadedFileProperties(Properties fileProperties) {
		// load the BBT preferences
		int i = 0;
		while(fileProperties.containsKey(BBT_SYMPTHOM+i)) {
			String sympthom = fileProperties.getProperty(BBT_SYMPTHOM+i);

			Vector<String> values = new Vector<String>();

			int j=0;
			while(fileProperties.containsKey(BBT_SYMPTHOM+i+BBT_VALUE+j)) {
				values.add(fileProperties.getProperty(BBT_SYMPTHOM+i+BBT_VALUE+j));
				j++;
			}
			sympthomsDefinition.setSypmthomNameAndValues(i, sympthom, values);
			i++;
		}
		String celsiusScale = fileProperties.getProperty(CELSIUS_SCALE, TRUE);
		fileBBTCelsiusScale = celsiusScale.equals(TRUE);

		String minTempScaleString = fileProperties.getProperty(SCALE_TEMP_MIN, ""); //$NON-NLS-1$
		String maxTempScaleString = fileProperties.getProperty(SCALE_TEMP_MAX, ""); //$NON-NLS-1$

		if(minTempScaleString.equals("")) { //$NON-NLS-1$
		    minTempScale = fileBBTCelsiusScale?DEFAULT_BBT_TEMPERATURE_MIN_C:DEFAULT_BBT_TEMPERATURE_MIN_F;
		} else {
		    minTempScale = Integer.parseInt(minTempScaleString);
		}

        if(maxTempScaleString.equals("")) { //$NON-NLS-1$
            maxTempScale = fileBBTCelsiusScale?
                    (DEFAULT_BBT_TEMPERATURE_MIN_C+DEFAULT_BBT_TEMPERATURE_LEVELS_C)
                    :(DEFAULT_BBT_TEMPERATURE_MIN_F+DEFAULT_BBT_TEMPERATURE_LEVELS_F);
        } else {
            maxTempScale = Integer.parseInt(maxTempScaleString);
        }
	}

	/**
	 * Helper method for the loadFromFile
	 * @param doc - the XML document to be analyzed
	 * @return FILE_TYPE_PLAIN if the document is proper plain not encrypted one,
	 * FILE_TYPE_ENCRYPTED if the document is proper encrypted one,
	 * FILE_TYPE_UNKNOWN if the document is improper or have wrong version
	 */
	private int getDocumentType(Document doc)  {

		int fileType = FILE_TYPE_UNKNOWN;

		NodeList nl = doc.getElementsByTagName("EncryptedData"); //$NON-NLS-1$
		if(nl.getLength() == 1) {
			fileType = FILE_TYPE_ENCRYPTED;
		} else {
			// check for the root tag first
			Node n = doc.getFirstChild();
			if(n.getNodeName().equals("pcalendar")) { //$NON-NLS-1$
				nl = doc.getElementsByTagName("version"); //$NON-NLS-1$
				if(nl.getLength() == 1) {
					String file_version = nl.item(0).getTextContent();
					if(file_version != null) {
						if(file_version.equals(FILE_VERSION)) {
							fileType = FILE_TYPE_PLAIN;
						}
					}
				}
			}
		}

		return fileType;
	}

	/**
	 * Save the current data to the file specified.
	 * @param file - a file reference
	 * @param password - a password to be applied to the file. If null -
	 * the file will be saved with the original password (or unencrypted as applicable).
	 * If empty string - the password will be removed and the file will be unencrypted.
	 * @return true on success
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void saveToFile(final File file, final char[] password) throws ParserConfigurationException, IOException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

		Element parent_element = doc.createElement("pcalendar"); //$NON-NLS-1$
		doc.appendChild(parent_element);

		Element element = doc.createElement("version"); //$NON-NLS-1$
		element.appendChild(doc.createTextNode(FILE_VERSION));
		parent_element.appendChild(element);

		element = doc.createElement("data"); //$NON-NLS-1$
		parent_element.appendChild(element);
		parent_element = element;

		saveToFileDates(doc, parent_element);
		saveToFileDateValues(doc, parent_element);
		saveToFileFileProperties(doc, parent_element);

		SecretKey secretKey = null;

		if(password != null) {
			// new password was specified
			if(password.length > 0) {
				secretKey = EngineXMLCrypter.generateSecretKey(password);
			}
		} else {
			secretKey = workFileKey;
		}

		if(secretKey != null) {
			doc = EngineXMLCrypter.encryptXML(doc, secretKey);
		}

		GZIPOutputStream zippedFile;

		FileOutputStream fileStream = new FileOutputStream(file);

		zippedFile =  new GZIPOutputStream(fileStream);

		// OutputFormat formatter = new OutputFormat();
		// formatter.setPreserveSpace(true);
		// XMLSerializer serializer =
		// 		new XMLSerializer(zippedFile, formatter);
		// serializer.serialize(doc);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(zippedFile);

		transformer.transform(source, result);


		zippedFile.finish();

		setWorkFile(file);
		workFileKey = secretKey;
		modified = false;
	}

	/**
	 * A short wrapper for the main saveToFile method. Null password is used.
	 * @param file
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void saveToFile(final File file) throws ParserConfigurationException, IOException, TransformerException {
		saveToFile(file, null);
	}

	/**
	 * Save to file date records
	 * saveToFile helper
	 * @param doc
	 * @param parent_element
	 */
	private void saveToFileDates(Document doc, Element parent_element) {
		Element element;
		for(Date d: startDates) {
			int type = dateTypes.get(d);
			if(dateTypeToTagMap.containsKey(type)) {
				element = doc.createElement(dateTypeToTagMap.get(type));
				element.appendChild(doc.createTextNode(DATE_FORMAT.format(d)));
				parent_element.appendChild(element);
			}
		}
	}

	/**
	 * Save to file the Date-Value pairs
	 * saveToFile helper
	 * @param doc
	 * @param parent_element
	 */
	private void saveToFileDateValues(Document doc, Element parent_element) {
		for(Integer valueTypeID: dateValueTypeToTagMap.keySet()) {
			Element element;
			Set<Date> dateSet = null;
			if(valueTypeID.equals(PCAL_TYPE_NOTES)) {
				dateSet = notesContainer.keySet();
			} else
				if(valueTypeID.equals(PCAL_TYPE_BBT)) {
					dateSet = bBTList.keySet();
				} else
					if(valueTypeID.equals(PCAL_TYPE_DATE_INTS)) {
						dateSet = dateIntValues.keySet();
					}

			for(Date d: dateSet) {
				element = doc.createElement(dateValueTypeToTagMap.get(valueTypeID));
				Element e1 =doc.createElement("date"); //$NON-NLS-1$
				e1.appendChild(doc.createTextNode(DATE_FORMAT.format(d)));
				element.appendChild(e1);

				e1 =doc.createElement("text"); //$NON-NLS-1$

				String text = ""; //$NON-NLS-1$
				if(valueTypeID.equals(PCAL_TYPE_NOTES)) {
					text = notesContainer.get(d);
				} else
					if(valueTypeID.equals(PCAL_TYPE_BBT)) {
						text = bBTList.get(d).toString();
					} else
						if(valueTypeID.equals(PCAL_TYPE_DATE_INTS)) {
							text = dateIntValues.get(d).toString();
						}

				e1.appendChild(doc.createTextNode(text));
				element.appendChild(e1);

				parent_element.appendChild(element);
			}
		}
	}

	/**
	 * Save to file the associated properties
	 * @param doc
	 * @param parent_element
	 * @throws IOException
	 */
	private void saveToFileFileProperties(Document doc, Element parent_element) throws IOException {
		Properties fileProperties = new Properties();

		int i = 0;
		for(String sympthom: sympthomsDefinition.getSympthoms()) {
			if(sympthom!=null) {
				fileProperties.setProperty(BBT_SYMPTHOM+i, sympthom);
				int j = 0;
				for(String value: sympthomsDefinition.getSympthomValues(i)) {
					fileProperties.setProperty(BBT_SYMPTHOM+i+BBT_VALUE+j, value);
					j++;
				}
			}
			i++;
		}

		fileProperties.setProperty(CELSIUS_SCALE, fileBBTCelsiusScale?TRUE:FALSE);
		fileProperties.setProperty(SCALE_TEMP_MIN, String.valueOf(minTempScale));
		fileProperties.setProperty(SCALE_TEMP_MAX, String.valueOf(maxTempScale));

		ByteArrayOutputStream bas = new ByteArrayOutputStream();
		fileProperties.store(bas, ""); //$NON-NLS-1$

		Element element = doc.createElement(FILE_PROPERTIES_TAG);
		element.appendChild(doc.createTextNode(bas.toString()));

		parent_element.appendChild(element);
	}


	/**
	 * Import list of menstruation dates from a plain text file (Lorg 1.x file version)
	 * @param fname name of the file
	 * @return true on success
	 * @throws IOException
	 * @throws ParseException
	 */
	public boolean importFromFile(final File file) throws IOException, ParseException
	{
		BufferedReader inputStream = null;

		try {
			inputStream =
					new BufferedReader(new FileReader(file));
			String l;
			while ((l = inputStream.readLine()) != null) {
				Date d = DATE_FORMAT.parse(l);
				startDates.add(d);
				dateTypes.put(d, PCAL_TYPE_MENS);
			}
		}
		finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		modified = true;
		recalculate();
		return true;
	}

	/**
	 * Export list of menstruation dates to a plain text file (Lorg 1.x file version)
	 * @param fname name of the file
	 * @return true on success
	 * @throws IOException
	 */
	public void exportToFile(final File file) throws IOException
	{
		PrintWriter outputStream = null;

		try {
			outputStream =
					new PrintWriter(new FileWriter(file));

			for(Date d : startDates) {
				if(dateTypes.get(d) == PCAL_TYPE_MENS) {
					outputStream.println(DATE_FORMAT.format(d));
				}
			}
		}
		finally {
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}

	public void addStartDate(final Date date)
	{
		startDates.add(date);
		dateTypes.put(date, PCAL_TYPE_MENS);
		modified = true;
		recalculate();
	}
	public void addPregnancyDate(final Date date)
	{
		startDates.add(date);
		dateTypes.put(date, PCAL_TYPE_PREGNANCY);
		modified = true;
		recalculate();
	}

	public void addPregnancyInterruptDate(final Date date)
	{
		startDates.add(date);
		dateTypes.put(date, PCAL_TYPE_PREGNANCY_INT);
		modified = true;
		recalculate();
	}

	public void addBirthDate(final Date date)
	{
		startDates.add(date);
		dateTypes.put(date, PCAL_TYPE_BIRTH);
		modified = true;
		recalculate();
	}

	public void addOvulationDate(final Date date) {
		startDates.add(date);
		dateTypes.put(date, PCAL_TYPE_OVULATION);
		modified = true;
		recalculate();
	}

	public void removeDateRecord(final Date date)
	{
		startDates.remove(date);
		dateTypes.remove(date);
		modified = true;
		recalculate();
	}

	/**
	 * This method adds a BBT record to the list
	 * @param date - date of the record
	 * @param i - a temperature in Celsius or Fahrenheit multiplied by 10
	 */
	public void addBBT(final Date date, int i) {
		bBTList.put(date, i);
		modified = true;
	}

	/**
	 * Get the BBT for the date specified
	 * @param date - the date to be queried
	 * @return - a BBT recorded for the date specified. 0 if there is no information
	 */
	public int getBBT(final Date date) {
		if(bBTList.containsKey(date)) {
			return bBTList.get(date);
		} else {
			return 0;
		}
	}

	/**
	 * Remove the BBT record for the date specified
	 * @param date - a date to be cleared
	 */
	public void removeBBT(final Date date) {
		bBTList.remove(date);
		modified = true;
	}

	/**
	 * This method associates an integer with the specified date
	 * @param date - date of the record
	 * @param index - an integer index within the integers container
	 * @param value - a value to be set
	 */
	public void setDateInteger(final Date date, final int index, Integer value) {
		DateIntsContainer container = dateIntValues.get(date);
		if(container == null) {
			dateIntValues.put(date, new DateIntsContainer(index, value));
		} else {
			container.setIntValue(index, value);
		}
		modified = true;
	}

	/**
	 * This method associates an integers record with the specified date
	 * @param date - date of the record
	 * @param record - a value to be set
	 */
	public void setDateIntegers(final Date date, DateIntsContainer record) {
		dateIntValues.put(date, record);
		modified = true;
	}


	/**
	 * This method returns an integers record associated with the specified date
	 * @param date - the date to be queried
	 * @return - an integers record associated with the date specified or null if nothing found
	 */
	public DateIntsContainer getDateIntegers(final Date date) {
		if(dateIntValues.containsKey(date)) {
			return dateIntValues.get(date);
		} else {
			return null;
		}
	}

	/**
	 * This method returns a date integers container for the fast internal operations
	 * @return - an integers record associated with the date specified or null if nothing found
	 */
	public HashMap<Date, DateIntsContainer> getAllDateIntegers() {
		return dateIntValues;
	}

	/**
	 * Remove the integers record associated with the date specified
	 * @param date - a date to be cleared
	 */
	public void removeDateIntegers(final Date date) {
		dateIntValues.remove(date);
		modified = true;
	}

	public void newFile() {
		clear();
		sympthomsDefinition.setDefaults();
	}

	private void clear() {
		startDates.clear();
		dateTypes.clear();
		notesContainer.clear();
		bBTList.clear();
		dateIntValues.clear();
		sympthomsDefinition.clear();
		modified = false;
		setWorkFile(null);
		workFileKey = null;
		recalculate();
	}

	/**
	 * This function is called each time the statistics needs to be recalculated
	 */
	private void recalculate()
	{
		// at this point we have the sorted starting dates list
		periodsCount = 0;
		avgLength = 0;
		minLength = 0;
		maxLength = 0;
		int sumLength = 0;
		recordedOvulations = 0;
		minOvulationDayNum = 0;
		maxOvulationDayNum = 0;
		calMethodAccuracy = 3;
		if(startDates.size()<2) {
			// there are no even a single period
			return;
		}

		Date prevdate = null;

		// we need to calculate the average, min and max period length
		for(Date pstart: startDates)
		{
			if(prevdate == null) {
				prevdate = pstart;
				continue;
			}
			int pDateType = dateTypes.get(pstart);
			int prevDateType = dateTypes.get(prevdate);

			if(pDateType == PCAL_TYPE_OVULATION) {
				if(prevDateType == PCAL_TYPE_MENS) {
					int ovulDayNumber = dateDiff(prevdate, pstart);
					if(ovulDayNumber < enginePreferences.regularMaxLength) {
						if(ovulDayNumber > maxOvulationDayNum) maxOvulationDayNum = ovulDayNumber;
						if((ovulDayNumber < minOvulationDayNum)||
								(minOvulationDayNum == 0)) minOvulationDayNum = ovulDayNumber;
						recordedOvulations++;
					}
				}
				// keep prev date the same jump to the next date
				continue;
			}

			// skip the non-menstruation neighbor records
			if((pDateType != PCAL_TYPE_MENS)||
					(prevDateType != PCAL_TYPE_MENS)) {
				prevdate = pstart;
				continue;
			}
			// pstart and pend points to starting and ending date of the period
			int p_length = dateDiff(prevdate, pstart);

			if((p_length < enginePreferences.regularMinLength) ||
					(p_length > enginePreferences.regularMaxLength)) {
				// bad period should not be taken into account

			} else {
				periodsCount++;
				if(p_length>maxLength) maxLength = p_length;
				if((p_length<minLength) || minLength == 0) minLength = p_length;
				sumLength+=p_length;
			}
			prevdate = pstart;
		}
		if(periodsCount>0) avgLength = Math.round(sumLength/periodsCount);

		// calendar method accuracy depends on the medium period length,
		// the difference between the longest and the shortest periodsCount
		// and the number of the recorded periods
		// 0 - high, 1 - moderate, 2 - low
		// for the high accuracy the conditions are almost unreal
		// Additional buffer days may increase or decrease accuracy
		if((periodsCount>RELIABLE_PERIODS_COUNT) &&
				(avgLength>(RELIABLE_MIN_AVG_LENGTH-enginePreferences.bufferDays)) &&
				(avgLength<(RELIABLE_MAX_AVG_LENGTH+enginePreferences.bufferDays)) &&
				((maxLength - minLength)<(RELIABLE_LENGTH_DIFF+enginePreferences.bufferDays)) &&
				(recordedOvulations > 12))
			calMethodAccuracy = 0;
		else
			if((periodsCount<RELIABLE_PERIODS_COUNT) ||
					((maxLength - minLength)>(UNRELIABLE_LENGTH_DIFF+enginePreferences.bufferDays)) ||
					((avgLength<(UNRELIABLE_MIN_AVG_LENGTH-enginePreferences.bufferDays)) &&
							(avgLength>(UNRELIABLE_MAX_AVG_LENGTH+enginePreferences.bufferDays))))
				calMethodAccuracy = 2;
			else
				calMethodAccuracy = 1;
	}

	public final Vector<PCalPeriodInfo> getPeriodsStats()
	{
		Vector<PCalPeriodInfo> result = new Vector<PCalPeriodInfo>();

		if(startDates.size()>1) {
			Date prevdate = null;

			for(Date pstart: startDates)
			{
				if(prevdate == null) {
					prevdate = pstart;
					continue;
				}

				int pDateType = dateTypes.get(pstart);

				if(pDateType == PCAL_TYPE_OVULATION) {
					// keep prev date the same jump to the next pointer date
					continue;
				}


				// skip the non-menstruation neighbor records
				if((pDateType != PCAL_TYPE_MENS)||
						(dateTypes.get(prevdate) != PCAL_TYPE_MENS)) {
					prevdate = pstart;
					continue;
				}

				int p_length = dateDiff(prevdate, pstart);

				if((p_length < enginePreferences.regularMinLength) ||
						(p_length > enginePreferences.regularMaxLength)) {
					// bad period should not be taken into account
				} else {
					PCalPeriodInfo pinfo = new PCalPeriodInfo();
					pinfo.startDate = prevdate;
					pinfo.endDate = pstart;
					pinfo.length = p_length;
					result.add(pinfo);
				}
				prevdate = pstart;
			}
		}

		return result;
	}

	/**
	 * @return Last menstruation date or null if none
	 */
	public final Date getLastMenstruationDate()
	{
		if(startDates.size()>0) {
			Date last_date = startDates.last();
			if(dateTypes.get(last_date) == PCAL_TYPE_MENS) {
				return last_date;
			}
			return null;
		} else {
			return null;
		}
	}

	/**
	 * @param date
	 * @return note for the specified date or null if none
	 */
	public String getDateNote(final Date date)
	{
		return notesContainer.get(date);
	}

	/**
	 * @param date
	 * @return true if the note exists for the date
	 */
	public boolean existsDateNote(final Date date)
	{
		return notesContainer.containsKey(date);
	}

	public void setDateNote(final Date date, final String note)
	{
		// need to check if any changes not to fall to the modified state
		// without a reason
		String old_note = notesContainer.get(date);
		if(old_note!=null) {
			if(old_note.equals(note)) {
				return;
			}
		}
		notesContainer.put(date, note);
		modified = true;
	}

	/**
	 * @return the list of the dates which have notes
	 */
	public final Set<Date> getNoteDates()
	{
		return notesContainer.keySet();
	}

	public void removeDateNote(final Date date)
	{
		notesContainer.remove(date);
		modified = true;
	}

	/** the difference in days between 2 dates within the Gregorian calendar - <b>after</b> 1582
	 * @param earlier the start date
	 * @param later the end date
	 * @return number of days
	 */
	public static int dateDiff( java.util.Date earlier, java.util.Date later ) {
		java.util.GregorianCalendar start = new
				java.util.GregorianCalendar();
		java.util.GregorianCalendar finish = new
				java.util.GregorianCalendar();
		if (later.compareTo( earlier ) > 0 ) { // this Date is after the Date argument
			start.setTime(later);
			finish.setTime(earlier);
		} else
			if (later.compareTo( earlier ) < 0 ) { // this Date is	before the Date argument
				start.setTime(earlier);
				finish.setTime(later);
			} else {
				return 0; // must be the same date
			}
		int days = 1;
		//if dates in the same year
		if ( start.get(Calendar.YEAR) == finish.get(Calendar.YEAR) ) {
			days = start.get(Calendar.DAY_OF_YEAR) -
					finish.get(Calendar.DAY_OF_YEAR);
		} else {
			days = start.get(Calendar.DAY_OF_YEAR); // number of days in the current year
			while ( start.get(Calendar.YEAR) - 1 > finish.get(Calendar.YEAR)
					) {
				start.add(Calendar.YEAR, -1); // keep counting back till they are the same year
				days += 365;
				if ( start.isLeapYear(start.get(Calendar.YEAR) ) ) {
					days++;
				}
			}
			start.add(Calendar.YEAR, -1); // get the days in the last year
			days += 365 - finish.get(Calendar.DAY_OF_YEAR);
			if ( start.isLeapYear(start.get(Calendar.YEAR) ) )
				days++;
		}
		return days;
	}

	/**
	 * Get the predicted birthday for a specified date and pregnancy day number
	 * @param date - currently selected date
	 * @param pregnancyDayNumber - the corresponding pregnancy day number
	 * @return predicted date of the birth
	 */
	public Date getPredictedBirthDay(Date date, int pregnancyDayNumber) {
		java.util.GregorianCalendar calendar = new java.util.GregorianCalendar();

		calendar.setTime(date);
		calendar.add(Calendar.DATE, REGULAR_PREGNANCY_DAYS - pregnancyDayNumber);
		return calendar.getTime();
	}

	/**
	 * @return the modified
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * @return the calMethodAccuracy
	 */
	public int getCalMethodAccuracy() {
		return calMethodAccuracy;
	}

	/**
	 * @return the workFileName
	 */
	public final File getWorkFile() {
		return workFile;
	}

	/**
	 * Sets workFile
	 * @param f - a file reference
	 */
	private void setWorkFile(File f) {
		workFile = f;
	}


	/**
	 * @return the avgLength
	 */
	public int getAvgLength() {
		return avgLength;
	}

	/**
	 * @return the maxLength
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * @return the minLength
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * @return the periodsCount
	 */
	public int getPeriodsCount() {
		return periodsCount;
	}

	/**
	 * @return the enginePreferences
	 */
	public final EnginePreferences getEnginePreferences() {
		return new EnginePreferences(enginePreferences);
	}

	/**
	 * Set preferences, save them and recalculate the internal data
	 * @param enginePreferences the enginePreferences to set
	 */
	public void setEnginePreferences(EnginePreferences enginePreferences) {

		this.enginePreferences = new EnginePreferences(enginePreferences);

		Preferences enginePrefsNode = PCalendar.settings.node("Engine"); //$NON-NLS-1$
		enginePrefsNode.putInt("Buffer Days", enginePreferences.bufferDays); //$NON-NLS-1$
		enginePrefsNode.putInt("Bad Feels Before", enginePreferences.badFeelDaysBefore); //$NON-NLS-1$
		enginePrefsNode.putInt("Bad Feels After", enginePreferences.badFeelDaysAfter); //$NON-NLS-1$
		enginePrefsNode.putInt("Reg. Min Length", enginePreferences.regularMinLength); //$NON-NLS-1$
		enginePrefsNode.putInt("Reg. Max Length", enginePreferences.regularMaxLength); //$NON-NLS-1$
		recalculate();
	}

	/**
	 * This method goes through the BBT records list and adjusts the temperatures either to Celsius or to Fahrenheit
	 * @param toCelsiusScale - if true, the conversion is performed from Celsius to Fahrenheit, if false - vice versa
	 */
	public void changeBBTScale(final boolean toCelsiusScale) {

		// check if anything needs to be done
		if(toCelsiusScale == fileBBTCelsiusScale) return;

		for(Date date: bBTList.keySet()) {
			bBTList.put(date,
					convertTemperatureScale(toCelsiusScale, bBTList.get(date)));
		}

		minTempScale = convertTemperatureScale(toCelsiusScale, minTempScale);
		maxTempScale = convertTemperatureScale(toCelsiusScale, maxTempScale);

		fileBBTCelsiusScale = toCelsiusScale;
		modified = true;
	}

	/**
	 * Converts temperatures between Celsius and Fahrenheit scales
	 * @param toCelsiusScale - if true, the conversion is performed from Celsius to Fahrenheit, if false - vice versa
	 * @param integer - a temperature value multiplied by 10
	 * @return a temperature value converted to the opposite scale and multiplied by 10
	 */
	public static int convertTemperatureScale(final boolean toCelsiusScale,
			final int source) {
		if(toCelsiusScale) {
			return java.lang.Math.round((source - 320)*5.0f/9.0f);
		} else {
			return java.lang.Math.round(source*9.0f/5.0f+320);
		}
	}

	/**
	 * Returns if the file BBT temperatures are in the Celsius scale
	 * @return the fileBBTCelsiusScale
	 */
	public boolean isFileBBTCelsiusScale() {
		return fileBBTCelsiusScale;
	}

	/**
	 * Returns the minumum of the temperature scale
     * @return the minTempScale
     */
    public int getMinTempScale() {
        return minTempScale;
    }

    /**
     * Sets minimum for the temperature scale
     * @param minTempScale the minTempScale to set
     */
    public void setMinTempScale(int minTempScale) {
        this.minTempScale = minTempScale;
        modified = true;
    }

    /**
     * Gets maximum for the temperature scale
     * @return the maxTempScale
     */
    public int getMaxTempScale() {
        return maxTempScale;
    }

    /**
     * Sets maximum for the temperature scalse
     * @param maxTempScale the maxTempScale to set
     */
    public void setMaxTempScale(int maxTempScale) {
        this.maxTempScale = maxTempScale;
        modified = true;
    }

    /**
     * @return temperature levels count according to the current temperatures range selection
     */
    public int getBBTTemperatureLevelsCount () {
        return maxTempScale - minTempScale + 1;
    }

    /**
	 * @return the default engine preferences
	 */
	public EnginePreferences getDefaultPreferences() {
		EnginePreferences enginePreferences = new EnginePreferences();
		enginePreferences.bufferDays = DEFAULT_BUFFER_DAYS;
		enginePreferences.badFeelDaysBefore = DEFAULT_BAD_FEEL_DAYS_BEFORE;
		enginePreferences.badFeelDaysAfter = DEFAULT_BAD_FEEL_DAYS_AFTER;
		enginePreferences.regularMinLength = DEFAULT_REGULAR_MIN_LENGTH;
		enginePreferences.regularMaxLength = DEFAULT_REGULAR_MAX_LENGTH;
		return enginePreferences;
	}

	/**
	 * Engine does not care about these sympthoms. Feel free to modify this list as needed
	 * @return the reference to the container of the sympthoms to be monitored in addition to the BBT
	 */
	public BBTSympthomsSetDefinition getBBTSympthoms() {
		return sympthomsDefinition;
	}

	/**
	 * Swap positions of the two sympthoms
	 * @param sympthomIndex1 sympthom index #1
	 * @param sympthomIndex2 sympthom index #2
	 */
	public void swapSympthoms(int sympthomIndex1, int sympthomIndex2) {
		// change the names and values in the container
		String[] sympthomsStringList = sympthomsDefinition.getSympthoms();
		String tmpString = sympthomsStringList[sympthomIndex2];
		Vector<String>tmpVector = sympthomsDefinition.getSympthomValuesVector(sympthomIndex2);
		sympthomsDefinition.setSypmthomNameAndValues(sympthomIndex2,
				sympthomsStringList[sympthomIndex1],
				sympthomsDefinition.getSympthomValuesVector(sympthomIndex1));
		sympthomsDefinition.setSypmthomNameAndValues(sympthomIndex1,
				tmpString,tmpVector);

		// update the date-integers pairs according to this change
		for(DateIntsContainer intCnt: PCalendar.engine.getAllDateIntegers().values()) {
			if(intCnt == null) continue;
			Integer tempInt = intCnt.getIntValue(sympthomIndex2);
			intCnt.setIntValue(sympthomIndex2, intCnt.getIntValue(sympthomIndex1));
			intCnt.setIntValue(sympthomIndex1, tempInt);
		}
		modified = true;
	}

	/**
	 * Swaps two values of a single sympthom
	 * @param sympthomIndex
	 * @param valueIndex1
	 * @param valueIndex2
	 */
	public void swapSympthomValues(int sympthomIndex, int valueIndex1, int valueIndex2) {
		if((sympthomIndex >=0) && (sympthomIndex < sympthomsDefinition.getSympthoms().length)) {
			String tmpValue = sympthomsDefinition.getSympthomValue(sympthomIndex, valueIndex1);
			sympthomsDefinition.setSypmthomValue(sympthomIndex, valueIndex1, sympthomsDefinition.getSympthomValue(sympthomIndex, valueIndex2));
			sympthomsDefinition.setSypmthomValue(sympthomIndex, valueIndex2, tmpValue);

			// update the date-integets pairs according to this change
			for(DateIntsContainer intCnt: PCalendar.engine.getAllDateIntegers().values()) {
				Integer tempInt = intCnt.getIntValue(sympthomIndex);
				if(tempInt!=null) {
					if(tempInt.equals(valueIndex1)) {
						intCnt.setIntValue(sympthomIndex, valueIndex2);
					} else
						if(tempInt.equals(valueIndex2)) {
							intCnt.setIntValue(sympthomIndex, valueIndex1);
						}
				}
			}
			modified = true;
		}
	}

	/**
	 * Insert a new empty sympthom value to the position specified.
	 * @param sympthomIndex
	 * @param newValueIndex
	 */
	public void insertSypmthomValue(int sympthomIndex, int newValueIndex) {
		int insertedPosition = sympthomsDefinition.insertSypmthomValue(sympthomIndex, newValueIndex, ""); //$NON-NLS-1$
		if(insertedPosition == -1) return;

		applyDateIntDelta(sympthomIndex, newValueIndex, 1);

		modified = true;
	}

	/**
	 * Deletes the sympthom value from the definition and updates the file data
	 * @param sympthomIndex
	 * @param deletedValueIndex
	 */
	public void deleteSypmthomValue(int sympthomIndex,	int deletedValueIndex) {
		sympthomsDefinition.getSympthomValuesVector(sympthomIndex).remove(deletedValueIndex);

		if(deletedValueIndex>0) {
			applyDateIntDelta(sympthomIndex, deletedValueIndex, -1);
		} else {
			applyDateIntDelta(sympthomIndex, deletedValueIndex+1, -1);
		}

		modified = true;
	}

	/**
	 * This is a helper method for shifting all date-ints up or down when a sympthom value is added or deleted
	 * @param sympthomIndex
	 * @param startingValue apply to values equal or larger than this
	 * @param delta
	 */
	private void applyDateIntDelta(int sympthomIndex, int startingValue, int delta) {
		for(DateIntsContainer intCnt: PCalendar.engine.getAllDateIntegers().values()) {
			Integer tempInt = intCnt.getIntValue(sympthomIndex);
			if(tempInt!=null) {
				if(tempInt >= startingValue) {
					intCnt.setIntValue(sympthomIndex, tempInt + delta);
				}
			}
		}
	}
}
