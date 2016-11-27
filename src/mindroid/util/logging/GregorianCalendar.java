/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package mindroid.util.logging;

/**
 * {@code GregorianCalendar} is a concrete subclass of {@link Calendar} and provides the standard
 * calendar used by most of the world.
 * 
 * <p>
 * The standard (Gregorian) calendar has 2 eras, BC and AD.
 * 
 * <p>
 * This implementation handles a single discontinuity, which corresponds by default to the date the
 * Gregorian calendar was instituted (October 15, 1582 in some countries, later in others). The
 * cutover date may be changed by the caller by calling {@code setGregorianChange()}.
 * 
 * <p>
 * Historically, in those countries which adopted the Gregorian calendar first, October 4, 1582 was
 * thus followed by October 15, 1582. This calendar models this correctly. Before the Gregorian
 * cutover, {@code GregorianCalendar} implements the Julian calendar. The only difference between
 * the Gregorian and the Julian calendar is the leap year rule. The Julian calendar specifies leap
 * years every four years, whereas the Gregorian calendar omits century years which are not
 * divisible by 400.
 * 
 * <p>
 * {@code GregorianCalendar} implements <em>proleptic</em> Gregorian and Julian calendars. That is,
 * dates are computed by extrapolating the current rules indefinitely far backward and forward in
 * time. As a result, {@code GregorianCalendar} may be used for all years to generate meaningful and
 * consistent results. However, dates obtained using {@code GregorianCalendar} are historically
 * accurate only from March 1, 4 AD onward, when modern Julian calendar rules were adopted. Before
 * this date, leap year rules were applied irregularly, and before 45 BC the Julian calendar did not
 * even exist.
 * 
 * <p>
 * Prior to the institution of the Gregorian calendar, New Year's Day was March 25. To avoid
 * confusion, this calendar always uses January 1. A manual adjustment may be made if desired for
 * dates that are prior to the Gregorian changeover and which fall between January 1 and March 24.
 * 
 * <p>
 * Values calculated for the {@code WEEK_OF_YEAR} field range from 1 to 53. Week 1 for a year is the
 * earliest seven day period starting on {@code getFirstDayOfWeek()} that contains at least
 * {@code getMinimalDaysInFirstWeek()} days from that year. It thus depends on the values of
 * {@code getMinimalDaysInFirstWeek()}, {@code getFirstDayOfWeek()}, and the day of the week of
 * January 1. Weeks between week 1 of one year and week 1 of the following year are numbered
 * sequentially from 2 to 52 or 53 (as needed).
 * 
 * <p>
 * For example, January 1, 1998 was a Thursday. If {@code getFirstDayOfWeek()} is {@code MONDAY} and
 * {@code getMinimalDaysInFirstWeek()} is 4 (these are the values reflecting ISO 8601 and many
 * national standards), then week 1 of 1998 starts on December 29, 1997, and ends on January 4,
 * 1998. If, however, {@code getFirstDayOfWeek()} is {@code SUNDAY}, then week 1 of 1998 starts on
 * January 4, 1998, and ends on January 10, 1998; the first three days of 1998 then are part of week
 * 53 of 1997.
 * 
 * <p>
 * Values calculated for the {@code WEEK_OF_MONTH} field range from 0 or 1 to 4 or 5. Week 1 of a
 * month (the days with <code>WEEK_OF_MONTH =
 * 1</code>) is the earliest set of at least {@code getMinimalDaysInFirstWeek()} contiguous days in
 * that month, ending on the day before {@code getFirstDayOfWeek()}. Unlike week 1 of a year, week 1
 * of a month may be shorter than 7 days, need not start on {@code getFirstDayOfWeek()}, and will
 * not include days of the previous month. Days of a month before week 1 have a
 * {@code WEEK_OF_MONTH} of 0.
 * 
 * <p>
 * For example, if {@code getFirstDayOfWeek()} is {@code SUNDAY} and
 * {@code getMinimalDaysInFirstWeek()} is 4, then the first week of January 1998 is Sunday, January
 * 4 through Saturday, January 10. These days have a {@code WEEK_OF_MONTH} of 1. Thursday, January 1
 * through Saturday, January 3 have a {@code WEEK_OF_MONTH} of 0. If
 * {@code getMinimalDaysInFirstWeek()} is changed to 3, then January 1 through January 3 have a
 * {@code WEEK_OF_MONTH} of 1.
 * 
 * <p>
 * <strong>Example:</strong> <blockquote>
 * 
 * <pre>
 * // get the supported ids for GMT-08:00 (Pacific Standard Time)
 * String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
 * // if no ids were returned, something is wrong. get out.
 * if (ids.length == 0) System.exit(0);
 * 
 * // begin output
 * System.out.println(&quot;Current Time&quot;);
 * 
 * // create a Pacific Standard Time time zone
 * SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
 * 
 * // set up rules for daylight savings time
 * pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
 * pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
 * 
 * // create a GregorianCalendar with the Pacific Daylight time zone
 * // and the current date and time
 * Calendar calendar = new GregorianCalendar(pdt);
 * Date trialTime = new Date();
 * calendar.setTime(trialTime);
 * 
 * // print out a bunch of interesting things
 * System.out.println(&quot;ERA: &quot; + calendar.get(Calendar.ERA));
 * System.out.println(&quot;YEAR: &quot; + calendar.get(Calendar.YEAR));
 * System.out.println(&quot;MONTH: &quot; + calendar.get(Calendar.MONTH));
 * System.out.println(&quot;WEEK_OF_YEAR: &quot; + calendar.get(Calendar.WEEK_OF_YEAR));
 * System.out.println(&quot;WEEK_OF_MONTH: &quot; + calendar.get(Calendar.WEEK_OF_MONTH));
 * System.out.println(&quot;DATE: &quot; + calendar.get(Calendar.DATE));
 * System.out.println(&quot;DAY_OF_MONTH: &quot; + calendar.get(Calendar.DAY_OF_MONTH));
 * System.out.println(&quot;DAY_OF_YEAR: &quot; + calendar.get(Calendar.DAY_OF_YEAR));
 * System.out.println(&quot;DAY_OF_WEEK: &quot; + calendar.get(Calendar.DAY_OF_WEEK));
 * System.out.println(&quot;DAY_OF_WEEK_IN_MONTH: &quot; + calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH));
 * System.out.println(&quot;AM_PM: &quot; + calendar.get(Calendar.AM_PM));
 * System.out.println(&quot;HOUR: &quot; + calendar.get(Calendar.HOUR));
 * System.out.println(&quot;HOUR_OF_DAY: &quot; + calendar.get(Calendar.HOUR_OF_DAY));
 * System.out.println(&quot;MINUTE: &quot; + calendar.get(Calendar.MINUTE));
 * System.out.println(&quot;SECOND: &quot; + calendar.get(Calendar.SECOND));
 * System.out.println(&quot;MILLISECOND: &quot; + calendar.get(Calendar.MILLISECOND));
 * System.out.println(&quot;ZONE_OFFSET: &quot; + (calendar.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000)));
 * System.out.println(&quot;DST_OFFSET: &quot; + (calendar.get(Calendar.DST_OFFSET) / (60 * 60 * 1000)));
 * 
 * System.out.println(&quot;Current Time, with hour reset to 3&quot;);
 * calendar.clear(Calendar.HOUR_OF_DAY); // so doesn't override
 * calendar.set(Calendar.HOUR, 3);
 * System.out.println(&quot;ERA: &quot; + calendar.get(Calendar.ERA));
 * System.out.println(&quot;YEAR: &quot; + calendar.get(Calendar.YEAR));
 * System.out.println(&quot;MONTH: &quot; + calendar.get(Calendar.MONTH));
 * System.out.println(&quot;WEEK_OF_YEAR: &quot; + calendar.get(Calendar.WEEK_OF_YEAR));
 * System.out.println(&quot;WEEK_OF_MONTH: &quot; + calendar.get(Calendar.WEEK_OF_MONTH));
 * System.out.println(&quot;DATE: &quot; + calendar.get(Calendar.DATE));
 * System.out.println(&quot;DAY_OF_MONTH: &quot; + calendar.get(Calendar.DAY_OF_MONTH));
 * System.out.println(&quot;DAY_OF_YEAR: &quot; + calendar.get(Calendar.DAY_OF_YEAR));
 * System.out.println(&quot;DAY_OF_WEEK: &quot; + calendar.get(Calendar.DAY_OF_WEEK));
 * System.out.println(&quot;DAY_OF_WEEK_IN_MONTH: &quot; + calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH));
 * System.out.println(&quot;AM_PM: &quot; + calendar.get(Calendar.AM_PM));
 * System.out.println(&quot;HOUR: &quot; + calendar.get(Calendar.HOUR));
 * System.out.println(&quot;HOUR_OF_DAY: &quot; + calendar.get(Calendar.HOUR_OF_DAY));
 * System.out.println(&quot;MINUTE: &quot; + calendar.get(Calendar.MINUTE));
 * System.out.println(&quot;SECOND: &quot; + calendar.get(Calendar.SECOND));
 * System.out.println(&quot;MILLISECOND: &quot; + calendar.get(Calendar.MILLISECOND));
 * System.out.println(&quot;ZONE_OFFSET: &quot; + (calendar.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000))); // in
 *                                                                                                 // hours
 * System.out.println(&quot;DST_OFFSET: &quot; + (calendar.get(Calendar.DST_OFFSET) / (60 * 60 * 1000))); // in
 *                                                                                                 // hours
 * </pre>
 * 
 * </blockquote>
 * 
 * @see Calendar
 * @see TimeZone
 */
class GregorianCalendar {
    /**
     * Value of the {@code MONTH} field indicating the first month of the year.
     */
    public static final int JANUARY = 0;

    /**
     * Value of the {@code MONTH} field indicating the second month of the year.
     */
    public static final int FEBRUARY = 1;

    /**
     * Value of the {@code MONTH} field indicating the third month of the year.
     */
    public static final int MARCH = 2;

    /**
     * Value of the {@code MONTH} field indicating the fourth month of the year.
     */
    public static final int APRIL = 3;

    /**
     * Value of the {@code MONTH} field indicating the fifth month of the year.
     */
    public static final int MAY = 4;

    /**
     * Value of the {@code MONTH} field indicating the sixth month of the year.
     */
    public static final int JUNE = 5;

    /**
     * Value of the {@code MONTH} field indicating the seventh month of the year.
     */
    public static final int JULY = 6;

    /**
     * Value of the {@code MONTH} field indicating the eighth month of the year.
     */
    public static final int AUGUST = 7;

    /**
     * Value of the {@code MONTH} field indicating the ninth month of the year.
     */
    public static final int SEPTEMBER = 8;

    /**
     * Value of the {@code MONTH} field indicating the tenth month of the year.
     */
    public static final int OCTOBER = 9;

    /**
     * Value of the {@code MONTH} field indicating the eleventh month of the year.
     */
    public static final int NOVEMBER = 10;

    /**
     * Value of the {@code MONTH} field indicating the twelfth month of the year.
     */
    public static final int DECEMBER = 11;

    /**
     * Value of the {@code MONTH} field indicating the thirteenth month of the year. Although
     * {@code GregorianCalendar} does not use this value, lunar calendars do.
     */
    public static final int UNDECIMBER = 12;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Sunday.
     */
    public static final int SUNDAY = 1;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Monday.
     */
    public static final int MONDAY = 2;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Tuesday.
     */
    public static final int TUESDAY = 3;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Wednesday.
     */
    public static final int WEDNESDAY = 4;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Thursday.
     */
    public static final int THURSDAY = 5;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Friday.
     */
    public static final int FRIDAY = 6;

    /**
     * Value of the {@code DAY_OF_WEEK} field indicating Saturday.
     */
    public static final int SATURDAY = 7;

    /**
     * Field number for {@code get} and {@code set} indicating the era, e.g., AD or BC in the Julian
     * calendar. This is a calendar-specific value; see subclass documentation.
     * 
     * @see GregorianCalendar#AD
     * @see GregorianCalendar#BC
     */
    public static final int ERA = 0;

    /**
     * Field number for {@code get} and {@code set} indicating the year. This is a calendar-specific
     * value; see subclass documentation.
     */
    public static final int YEAR = 1;

    /**
     * Field number for {@code get} and {@code set} indicating the month. This is a
     * calendar-specific value. The first month of the year is {@code JANUARY}; the last depends on
     * the number of months in a year.
     * 
     * @see #JANUARY
     * @see #FEBRUARY
     * @see #MARCH
     * @see #APRIL
     * @see #MAY
     * @see #JUNE
     * @see #JULY
     * @see #AUGUST
     * @see #SEPTEMBER
     * @see #OCTOBER
     * @see #NOVEMBER
     * @see #DECEMBER
     * @see #UNDECIMBER
     */
    public static final int MONTH = 2;

    /**
     * Field number for {@code get} and {@code set} indicating the week number within the current
     * year. The first week of the year, as defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1. Subclasses define the value of
     * {@code WEEK_OF_YEAR} for days before the first week of the year.
     * 
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public static final int WEEK_OF_YEAR = 3;

    /**
     * Field number for {@code get} and {@code set} indicating the week number within the current
     * month. The first week of the month, as defined by {@code getFirstDayOfWeek()} and
     * {@code getMinimalDaysInFirstWeek()}, has value 1. Subclasses define the value of
     * {@code WEEK_OF_MONTH} for days before the first week of the month.
     * 
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public static final int WEEK_OF_MONTH = 4;

    /**
     * Field number for {@code get} and {@code set} indicating the day of the month. This is a
     * synonym for {@code DAY_OF_MONTH}. The first day of the month has value 1.
     * 
     * @see #DAY_OF_MONTH
     */
    public static final int DATE = 5;

    /**
     * Field number for {@code get} and {@code set} indicating the day of the month. This is a
     * synonym for {@code DATE}. The first day of the month has value 1.
     * 
     * @see #DATE
     */
    public static final int DAY_OF_MONTH = 5;

    /**
     * Field number for {@code get} and {@code set} indicating the day number within the current
     * year. The first day of the year has value 1.
     */
    public static final int DAY_OF_YEAR = 6;

    /**
     * Field number for {@code get} and {@code set} indicating the day of the week. This field takes
     * values {@code SUNDAY}, {@code MONDAY}, {@code TUESDAY}, {@code WEDNESDAY}, {@code THURSDAY},
     * {@code FRIDAY}, and {@code SATURDAY}.
     * 
     * @see #SUNDAY
     * @see #MONDAY
     * @see #TUESDAY
     * @see #WEDNESDAY
     * @see #THURSDAY
     * @see #FRIDAY
     * @see #SATURDAY
     */
    public static final int DAY_OF_WEEK = 7;

    /**
     * Field number for {@code get} and {@code set} indicating the ordinal number of the day of the
     * week within the current month. Together with the {@code DAY_OF_WEEK} field, this uniquely
     * specifies a day within a month. Unlike {@code WEEK_OF_MONTH} and {@code WEEK_OF_YEAR}, this
     * field's value does <em>not</em> depend on {@code getFirstDayOfWeek()} or
     * {@code getMinimalDaysInFirstWeek()}. {@code DAY_OF_MONTH 1} through {@code 7} always
     * correspond to <code>DAY_OF_WEEK_IN_MONTH
     * 1</code>; {@code 8} through {@code 15} correspond to {@code DAY_OF_WEEK_IN_MONTH 2}, and so
     * on. {@code DAY_OF_WEEK_IN_MONTH 0} indicates the week before {@code DAY_OF_WEEK_IN_MONTH 1}.
     * Negative values count back from the end of the month, so the last Sunday of a month is
     * specified as {@code DAY_OF_WEEK = SUNDAY, DAY_OF_WEEK_IN_MONTH = -1}. Because negative values
     * count backward they will usually be aligned differently within the month than positive
     * values. For example, if a month has 31 days, {@code DAY_OF_WEEK_IN_MONTH -1} will overlap
     * {@code DAY_OF_WEEK_IN_MONTH 5} and the end of {@code 4}.
     * 
     * @see #DAY_OF_WEEK
     * @see #WEEK_OF_MONTH
     */
    public static final int DAY_OF_WEEK_IN_MONTH = 8;

    /**
     * Field number for {@code get} and {@code set} indicating whether the {@code HOUR} is before or
     * after noon. E.g., at 10:04:15.250 PM the {@code AM_PM} is {@code PM}.
     * 
     * @see #AM
     * @see #PM
     * @see #HOUR
     */
    public static final int AM_PM = 9;

    /**
     * Field number for {@code get} and {@code set} indicating the hour of the morning or afternoon.
     * {@code HOUR} is used for the 12-hour clock. E.g., at 10:04:15.250 PM the {@code HOUR} is 10.
     * 
     * @see #AM_PM
     * @see #HOUR_OF_DAY
     */
    public static final int HOUR = 10;

    /**
     * Field number for {@code get} and {@code set} indicating the hour of the day.
     * {@code HOUR_OF_DAY} is used for the 24-hour clock. E.g., at 10:04:15.250 PM the
     * {@code HOUR_OF_DAY} is 22.
     * 
     * @see #HOUR
     */
    public static final int HOUR_OF_DAY = 11;

    /**
     * Field number for {@code get} and {@code set} indicating the minute within the hour. E.g., at
     * 10:04:15.250 PM the {@code MINUTE} is 4.
     */
    public static final int MINUTE = 12;

    /**
     * Field number for {@code get} and {@code set} indicating the second within the minute. E.g.,
     * at 10:04:15.250 PM the {@code SECOND} is 15.
     */
    public static final int SECOND = 13;

    /**
     * Field number for {@code get} and {@code set} indicating the millisecond within the second.
     * E.g., at 10:04:15.250 PM the {@code MILLISECOND} is 250.
     */
    public static final int MILLISECOND = 14;

    /**
     * Field number for {@code get} and {@code set} indicating the raw offset from GMT in
     * milliseconds.
     */
    public static final int ZONE_OFFSET = 15;

    /**
     * Field number for {@code get} and {@code set} indicating the daylight savings offset in
     * milliseconds.
     */
    public static final int DST_OFFSET = 16;

    /**
     * This is the total number of fields in this calendar.
     */
    public static final int FIELD_COUNT = 17;

    /**
     * Value of the {@code AM_PM} field indicating the period of the day from midnight to just
     * before noon.
     */
    public static final int AM = 0;

    /**
     * Value of the {@code AM_PM} field indicating the period of the day from noon to just before
     * midnight.
     */
    public static final int PM = 1;

    /**
     * Requests both {@code SHORT} and {@code LONG} styles in the map returned by
     * {@link #getDisplayNames}.
     * 
     * @since 1.6
     */
    public static final int ALL_STYLES = 0;

    /**
     * Requests short names (such as "Jan") from {@link #getDisplayName} or {@link #getDisplayNames}
     * .
     * 
     * @since 1.6
     */
    public static final int SHORT = 1;

    /**
     * Requests long names (such as "January") from {@link #getDisplayName} or
     * {@link #getDisplayNames}.
     * 
     * @since 1.6
     */
    public static final int LONG = 2;

    /**
     * Contains broken-down field values for the current value of {@code time} if
     * {@code areFieldsSet} is true, or stale data corresponding to some previous value otherwise.
     * Accessing the fields via {@code get} will ensure the fields are up-to-date. The array length
     * is always {@code FIELD_COUNT}.
     */
    protected int[] fields;

    /**
     * Whether the corresponding element in {@code field[]} has been set. Initially, these are all
     * false. The first time the fields are computed, these are set to true and remain set even if
     * the data becomes stale: you <i>must</i> check {@code areFieldsSet} if you want to know
     * whether the value is up-to-date. Note that {@code isSet} is <i>not</i> a safe alternative to
     * accessing this array directly, and will likewise return stale data! The array length is
     * always {@code FIELD_COUNT}.
     */
    protected boolean[] isSet;

    /**
     * A time in milliseconds since January 1, 1970. See {@code isTimeSet}. Accessing the time via
     * {@code getTimeInMillis} will always return the correct value.
     */
    protected long time;

    transient int lastTimeFieldSet;

    transient int lastDateFieldSet;

    private boolean lenient;

    private int firstDayOfWeek;

    private int minimalDaysInFirstWeek;

    /**
     * True iff the values in {@code fields[]} correspond to {@code time}. Despite the name, this is
     * effectively "are the values in fields[] up-to-date?" --- {@code fields[]} may contain
     * non-zero values and {@code isSet[]} may contain {@code true} values even when
     * {@code areFieldsSet} is false. Accessing the fields via {@code get} will ensure the fields
     * are up-to-date.
     */
    protected boolean areFieldsSet;

    private static final long serialVersionUID = -8125100834729963327L;

    /**
     * Value for the BC era.
     */
    public static final int BC = 0;

    /**
     * Value for the AD era.
     */
    public static final int AD = 1;

    private static final long defaultGregorianCutover = -12219292800000l;

    private long gregorianCutover = defaultGregorianCutover;

    private transient int changeYear = 1582;

    private transient int julianSkew = ((changeYear - 2000) / 400) + julianError() - ((changeYear - 2000) / 100);

    static byte[] DaysInMonth = new byte[] { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private static int[] DaysInYear = new int[] { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };

    private static int[] maximums = new int[] { 1, 292278994, 11, 53, 6, 31, 366, 7, 6, 1, 11, 23, 59, 59, 999, 14 * 3600 * 1000, 7200000 };

    private static int[] minimums = new int[] { 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, -13 * 3600 * 1000, 0 };

    private static int[] leastMaximums = new int[] { 1, 292269054, 11, 50, 3, 28, 355, 7, 3, 1, 11, 23, 59, 59, 999, 50400000, 1200000 };

    private boolean isCached;

    private int[] cachedFields = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private long nextMidnightMillis = 0L;

    private long lastMidnightMillis = 0L;

    private int currentYearSkew = 10;

    private int lastYearSkew = 0;

    protected boolean isTimeSet;

    public GregorianCalendar() {
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        areFieldsSet = isTimeSet = false;
        setLenient(true);
        firstDayOfWeek = SUNDAY;
        setMinimalDaysInFirstWeek(1);
    }

    GregorianCalendar(long milliseconds) {
        fields = new int[FIELD_COUNT];
        isSet = new boolean[FIELD_COUNT];
        areFieldsSet = isTimeSet = false;
        setLenient(true);
        firstDayOfWeek = SUNDAY;
        setMinimalDaysInFirstWeek(1);
        setTimeInMillis(milliseconds);
    }

    private final void cachedFieldsCheckAndGet(long timeVal, long newTimeMillis, long newTimeMillisAdjusted, int millis, int zoneOffset) {
        int dstOffset = fields[DST_OFFSET];
        if (!isCached || newTimeMillis >= nextMidnightMillis || newTimeMillis <= lastMidnightMillis || cachedFields[4] != zoneOffset
                || (dstOffset == 0 && (newTimeMillisAdjusted >= nextMidnightMillis)) || (dstOffset != 0 && (newTimeMillisAdjusted <= lastMidnightMillis))) {
            fullFieldsCalc(timeVal, millis, zoneOffset);
            isCached = false;
        } else {
            fields[YEAR] = cachedFields[0];
            fields[MONTH] = cachedFields[1];
            fields[DATE] = cachedFields[2];
            fields[DAY_OF_WEEK] = cachedFields[3];
            fields[ERA] = cachedFields[5];
            fields[WEEK_OF_YEAR] = cachedFields[6];
            fields[WEEK_OF_MONTH] = cachedFields[7];
            fields[DAY_OF_YEAR] = cachedFields[8];
            fields[DAY_OF_WEEK_IN_MONTH] = cachedFields[9];
        }
    }

    private final void fullFieldsCalc(long timeVal, int millis, int zoneOffset) {
        long days = timeVal / 86400000;

        if (millis < 0) {
            millis += 86400000;
            days--;
        }
        // Cannot add ZONE_OFFSET to time as it might overflow
        millis += zoneOffset;
        while (millis < 0) {
            millis += 86400000;
            days--;
        }
        while (millis >= 86400000) {
            millis -= 86400000;
            days++;
        }

        int dayOfYear = computeYearAndDay(days, timeVal + zoneOffset);
        fields[DAY_OF_YEAR] = dayOfYear;
        if (fields[YEAR] == changeYear && gregorianCutover <= timeVal + zoneOffset) {
            dayOfYear += currentYearSkew;
        }
        int month = dayOfYear / 32;
        boolean leapYear = isLeapYear(fields[YEAR]);
        int date = dayOfYear - daysInYear(leapYear, month);
        if (date > daysInMonth(leapYear, month)) {
            date -= daysInMonth(leapYear, month);
            month++;
        }
        fields[DAY_OF_WEEK] = mod7(days - 3) + 1;
        int dstOffset = 0;
        if (fields[YEAR] > 0) {
            dstOffset -= zoneOffset;
        }
        fields[DST_OFFSET] = dstOffset;
        if (dstOffset != 0) {
            long oldDays = days;
            millis += dstOffset;
            if (millis < 0) {
                millis += 86400000;
                days--;
            } else if (millis >= 86400000) {
                millis -= 86400000;
                days++;
            }
            if (oldDays != days) {
                dayOfYear = computeYearAndDay(days, timeVal - zoneOffset + dstOffset);
                fields[DAY_OF_YEAR] = dayOfYear;
                if (fields[YEAR] == changeYear && gregorianCutover <= timeVal - zoneOffset + dstOffset) {
                    dayOfYear += currentYearSkew;
                }
                month = dayOfYear / 32;
                leapYear = isLeapYear(fields[YEAR]);
                date = dayOfYear - daysInYear(leapYear, month);
                if (date > daysInMonth(leapYear, month)) {
                    date -= daysInMonth(leapYear, month);
                    month++;
                }
                fields[DAY_OF_WEEK] = mod7(days - 3) + 1;
            }
        }

        fields[MILLISECOND] = (millis % 1000);
        millis /= 1000;
        fields[SECOND] = (millis % 60);
        millis /= 60;
        fields[MINUTE] = (millis % 60);
        millis /= 60;
        fields[HOUR_OF_DAY] = (millis % 24);
        fields[AM_PM] = fields[HOUR_OF_DAY] > 11 ? 1 : 0;
        fields[HOUR] = fields[HOUR_OF_DAY] % 12;

        if (fields[YEAR] <= 0) {
            fields[ERA] = BC;
            fields[YEAR] = -fields[YEAR] + 1;
        } else {
            fields[ERA] = AD;
        }
        fields[MONTH] = month;
        fields[DATE] = date;
        fields[DAY_OF_WEEK_IN_MONTH] = (date - 1) / 7 + 1;
        fields[WEEK_OF_MONTH] = (date - 1 + mod7(days - date - 2 - (getFirstDayOfWeek() - 1))) / 7 + 1;
        int daysFromStart = mod7(days - 3 - (fields[DAY_OF_YEAR] - 1) - (getFirstDayOfWeek() - 1));
        int week = (fields[DAY_OF_YEAR] - 1 + daysFromStart) / 7 + (7 - daysFromStart >= getMinimalDaysInFirstWeek() ? 1 : 0);
        if (week == 0) {
            fields[WEEK_OF_YEAR] = 7 - mod7(daysFromStart - (isLeapYear(fields[YEAR] - 1) ? 2 : 1)) >= getMinimalDaysInFirstWeek() ? 53 : 52;
        } else if (fields[DAY_OF_YEAR] >= (leapYear ? 367 : 366) - mod7(daysFromStart + (leapYear ? 2 : 1))) {
            fields[WEEK_OF_YEAR] = 7 - mod7(daysFromStart + (leapYear ? 2 : 1)) >= getMinimalDaysInFirstWeek() ? 1 : week;
        } else {
            fields[WEEK_OF_YEAR] = week;
        }
    }

    protected void computeFields() {
        int dstOffset = 0;
        int zoneOffset = 0;
        fields[DST_OFFSET] = dstOffset;
        fields[ZONE_OFFSET] = zoneOffset;

        int millis = (int) (time % 86400000);
        int savedMillis = millis;
        // compute without a change in daylight saving time
        int offset = zoneOffset + dstOffset;
        long newTime = time + offset;

        if (time > 0L && newTime < 0L && offset > 0) {
            newTime = 0x7fffffffffffffffL;
        } else if (time < 0L && newTime > 0L && offset < 0) {
            newTime = 0x8000000000000000L;
        }

        // FIXME: I don't think this caching ever really gets used, because it requires that the
        // time zone doesn't use daylight savings (ever). So unless you're somewhere like Taiwan...
        if (isCached) {
            if (millis < 0) {
                millis += 86400000;
            }

            // Cannot add ZONE_OFFSET to time as it might overflow
            millis += zoneOffset;
            millis += dstOffset;

            if (millis < 0) {
                millis += 86400000;
            } else if (millis >= 86400000) {
                millis -= 86400000;
            }

            fields[MILLISECOND] = (millis % 1000);
            millis /= 1000;
            fields[SECOND] = (millis % 60);
            millis /= 60;
            fields[MINUTE] = (millis % 60);
            millis /= 60;
            fields[HOUR_OF_DAY] = (millis % 24);
            millis /= 24;
            fields[AM_PM] = fields[HOUR_OF_DAY] > 11 ? 1 : 0;
            fields[HOUR] = fields[HOUR_OF_DAY] % 12;

            // FIXME: this has to be wrong; useDaylightTime doesn't mean what they think it means!
            long newTimeAdjusted = newTime;

            if (newTime > 0L && newTimeAdjusted < 0L && dstOffset == 0) {
                newTimeAdjusted = 0x7fffffffffffffffL;
            } else if (newTime < 0L && newTimeAdjusted > 0L && dstOffset != 0) {
                newTimeAdjusted = 0x8000000000000000L;
            }

            cachedFieldsCheckAndGet(time, newTime, newTimeAdjusted, savedMillis, zoneOffset);
        } else {
            fullFieldsCalc(time, savedMillis, zoneOffset);
        }

        for (int i = 0; i < FIELD_COUNT; i++) {
            isSet[i] = true;
        }

        // Caching
        if (!isCached && newTime != 0x7fffffffffffffffL && newTime != 0x8000000000000000L) {
            int cacheMillis = 0;

            cachedFields[0] = fields[YEAR];
            cachedFields[1] = fields[MONTH];
            cachedFields[2] = fields[DATE];
            cachedFields[3] = fields[DAY_OF_WEEK];
            cachedFields[4] = zoneOffset;
            cachedFields[5] = fields[ERA];
            cachedFields[6] = fields[WEEK_OF_YEAR];
            cachedFields[7] = fields[WEEK_OF_MONTH];
            cachedFields[8] = fields[DAY_OF_YEAR];
            cachedFields[9] = fields[DAY_OF_WEEK_IN_MONTH];

            cacheMillis += (23 - fields[HOUR_OF_DAY]) * 60 * 60 * 1000;
            cacheMillis += (59 - fields[MINUTE]) * 60 * 1000;
            cacheMillis += (59 - fields[SECOND]) * 1000;
            nextMidnightMillis = newTime + cacheMillis;

            cacheMillis = fields[HOUR_OF_DAY] * 60 * 60 * 1000;
            cacheMillis += fields[MINUTE] * 60 * 1000;
            cacheMillis += fields[SECOND] * 1000;
            lastMidnightMillis = newTime - cacheMillis;

            isCached = true;
        }
    }

    protected void computeTime() {
        if (!isLenient()) {
            if (isSet[HOUR_OF_DAY]) {
                if (fields[HOUR_OF_DAY] < 0 || fields[HOUR_OF_DAY] > 23) {
                    throw new IllegalArgumentException();
                }
            } else if (isSet[HOUR] && (fields[HOUR] < 0 || fields[HOUR] > 11)) {
                throw new IllegalArgumentException();
            }
            if (isSet[MINUTE] && (fields[MINUTE] < 0 || fields[MINUTE] > 59)) {
                throw new IllegalArgumentException();
            }
            if (isSet[SECOND] && (fields[SECOND] < 0 || fields[SECOND] > 59)) {
                throw new IllegalArgumentException();
            }
            if (isSet[MILLISECOND] && (fields[MILLISECOND] < 0 || fields[MILLISECOND] > 999)) {
                throw new IllegalArgumentException();
            }
            if (isSet[WEEK_OF_YEAR] && (fields[WEEK_OF_YEAR] < 1 || fields[WEEK_OF_YEAR] > 53)) {
                throw new IllegalArgumentException();
            }
            if (isSet[DAY_OF_WEEK] && (fields[DAY_OF_WEEK] < 1 || fields[DAY_OF_WEEK] > 7)) {
                throw new IllegalArgumentException();
            }
            if (isSet[DAY_OF_WEEK_IN_MONTH] && (fields[DAY_OF_WEEK_IN_MONTH] < 1 || fields[DAY_OF_WEEK_IN_MONTH] > 6)) {
                throw new IllegalArgumentException();
            }
            if (isSet[WEEK_OF_MONTH] && (fields[WEEK_OF_MONTH] < 1 || fields[WEEK_OF_MONTH] > 6)) {
                throw new IllegalArgumentException();
            }
            if (isSet[AM_PM] && fields[AM_PM] != AM && fields[AM_PM] != PM) {
                throw new IllegalArgumentException();
            }
            if (isSet[HOUR] && (fields[HOUR] < 0 || fields[HOUR] > 11)) {
                throw new IllegalArgumentException();
            }
            if (isSet[YEAR]) {
                if (isSet[ERA] && fields[ERA] == BC && (fields[YEAR] < 1 || fields[YEAR] > 292269054)) {
                    throw new IllegalArgumentException();
                } else if (fields[YEAR] < 1 || fields[YEAR] > 292278994) {
                    throw new IllegalArgumentException();
                }
            }
            if (isSet[MONTH] && (fields[MONTH] < 0 || fields[MONTH] > 11)) {
                throw new IllegalArgumentException();
            }
        }

        long timeVal;
        long hour = 0;
        if (isSet[HOUR_OF_DAY] && lastTimeFieldSet != HOUR) {
            hour = fields[HOUR_OF_DAY];
        } else if (isSet[HOUR]) {
            hour = (fields[AM_PM] * 12) + fields[HOUR];
        }
        timeVal = hour * 3600000;

        if (isSet[MINUTE]) {
            timeVal += ((long) fields[MINUTE]) * 60000;
        }
        if (isSet[SECOND]) {
            timeVal += ((long) fields[SECOND]) * 1000;
        }
        if (isSet[MILLISECOND]) {
            timeVal += fields[MILLISECOND];
        }

        long days;
        int year = isSet[YEAR] ? fields[YEAR] : 1970;
        if (isSet[ERA]) {
            // Always test for valid ERA, even if the Calendar is lenient
            if (fields[ERA] != BC && fields[ERA] != AD) {
                throw new IllegalArgumentException();
            }
            if (fields[ERA] == BC) {
                year = 1 - year;
            }
        }

        boolean weekMonthSet = isSet[WEEK_OF_MONTH] || isSet[DAY_OF_WEEK_IN_MONTH];
        boolean useMonth = (isSet[DATE] || isSet[MONTH] || weekMonthSet) && lastDateFieldSet != DAY_OF_YEAR;
        if (useMonth && (lastDateFieldSet == DAY_OF_WEEK || lastDateFieldSet == WEEK_OF_YEAR)) {
            if (isSet[WEEK_OF_YEAR] && isSet[DAY_OF_WEEK]) {
                useMonth = lastDateFieldSet != WEEK_OF_YEAR && weekMonthSet && isSet[DAY_OF_WEEK];
            } else if (isSet[DAY_OF_YEAR]) {
                useMonth = isSet[DATE] && isSet[MONTH];
            }
        }

        if (useMonth) {
            int month = fields[MONTH];
            year += month / 12;
            month %= 12;
            if (month < 0) {
                year--;
                month += 12;
            }
            boolean leapYear = isLeapYear(year);
            days = daysFromBaseYear(year) + daysInYear(leapYear, month);
            boolean useDate = isSet[DATE];
            if (useDate && (lastDateFieldSet == DAY_OF_WEEK || lastDateFieldSet == WEEK_OF_MONTH || lastDateFieldSet == DAY_OF_WEEK_IN_MONTH)) {
                useDate = !(isSet[DAY_OF_WEEK] && weekMonthSet);
            }
            if (useDate) {
                if (!isLenient() && (fields[DATE] < 1 || fields[DATE] > daysInMonth(leapYear, month))) {
                    throw new IllegalArgumentException();
                }
                days += fields[DATE] - 1;
            } else {
                int dayOfWeek;
                if (isSet[DAY_OF_WEEK]) {
                    dayOfWeek = fields[DAY_OF_WEEK] - 1;
                } else {
                    dayOfWeek = getFirstDayOfWeek() - 1;
                }
                if (isSet[WEEK_OF_MONTH] && lastDateFieldSet != DAY_OF_WEEK_IN_MONTH) {
                    int skew = mod7(days - 3 - (getFirstDayOfWeek() - 1));
                    days += (fields[WEEK_OF_MONTH] - 1) * 7 + mod7(skew + dayOfWeek - (days - 3)) - skew;
                } else if (isSet[DAY_OF_WEEK_IN_MONTH]) {
                    if (fields[DAY_OF_WEEK_IN_MONTH] >= 0) {
                        days += mod7(dayOfWeek - (days - 3)) + (fields[DAY_OF_WEEK_IN_MONTH] - 1) * 7;
                    } else {
                        days += daysInMonth(leapYear, month) + mod7(dayOfWeek - (days + daysInMonth(leapYear, month) - 3)) + fields[DAY_OF_WEEK_IN_MONTH] * 7;
                    }
                } else if (isSet[DAY_OF_WEEK]) {
                    int skew = mod7(days - 3 - (getFirstDayOfWeek() - 1));
                    days += mod7(mod7(skew + dayOfWeek - (days - 3)) - skew);
                }
            }
        } else {
            boolean useWeekYear = isSet[WEEK_OF_YEAR] && lastDateFieldSet != DAY_OF_YEAR;
            if (useWeekYear && isSet[DAY_OF_YEAR]) {
                useWeekYear = isSet[DAY_OF_WEEK];
            }
            days = daysFromBaseYear(year);
            if (useWeekYear) {
                int dayOfWeek;
                if (isSet[DAY_OF_WEEK]) {
                    dayOfWeek = fields[DAY_OF_WEEK] - 1;
                } else {
                    dayOfWeek = getFirstDayOfWeek() - 1;
                }
                int skew = mod7(days - 3 - (getFirstDayOfWeek() - 1));
                days += (fields[WEEK_OF_YEAR] - 1) * 7 + mod7(skew + dayOfWeek - (days - 3)) - skew;
                if (7 - skew < getMinimalDaysInFirstWeek()) {
                    days += 7;
                }
            } else if (isSet[DAY_OF_YEAR]) {
                if (!isLenient() && (fields[DAY_OF_YEAR] < 1 || fields[DAY_OF_YEAR] > (365 + (isLeapYear(year) ? 1 : 0)))) {
                    throw new IllegalArgumentException();
                }
                days += fields[DAY_OF_YEAR] - 1;
            } else if (isSet[DAY_OF_WEEK]) {
                days += mod7(fields[DAY_OF_WEEK] - 1 - (days - 3));
            }
        }
        lastDateFieldSet = 0;

        timeVal += days * 86400000;
        // Use local time to compare with the gregorian change
        if (year == changeYear && timeVal >= gregorianCutover + julianError() * 86400000L) {
            timeVal -= julianError() * 86400000L;
        }

        // It is not possible to simply subtract getOffset(timeVal) from timeVal
        // to get UTC.
        // The trick is needed for the moment when DST transition occurs,
        // say 1:00 is a transition time when DST offset becomes +1 hour,
        // then wall time in the interval 1:00 - 2:00 is invalid and is
        // treated as UTC time.
        long timeValWithoutDST = timeVal - getOffset(timeVal);
        timeVal -= getOffset(timeValWithoutDST);
        // Need to update wall time in fields, since it was invalid due to DST
        // transition
        this.time = timeVal;
        if (timeValWithoutDST != timeVal) {
            computeFields();
            areFieldsSet = true;
        }
    }

    private int computeYearAndDay(long dayCount, long localTime) {
        int year = 1970;
        long days = dayCount;
        if (localTime < gregorianCutover) {
            days -= julianSkew;
        }
        int approxYears;

        while ((approxYears = (int) (days / 365)) != 0) {
            year = year + approxYears;
            days = dayCount - daysFromBaseYear(year);
        }
        if (days < 0) {
            year = year - 1;
            days = days + daysInYear(year);
        }
        fields[YEAR] = year;
        return (int) days + 1;
    }

    private long daysFromBaseYear(int iyear) {
        long year = iyear;

        if (year >= 1970) {
            long days = (year - 1970) * 365 + ((year - 1969) / 4);
            if (year > changeYear) {
                days -= ((year - 1901) / 100) - ((year - 1601) / 400);
            } else {
                if (year == changeYear) {
                    days += currentYearSkew;
                } else if (year == changeYear - 1) {
                    days += lastYearSkew;
                } else {
                    days += julianSkew;
                }
            }
            return days;
        } else if (year <= changeYear) {
            return (year - 1970) * 365 + ((year - 1972) / 4) + julianSkew;
        }
        return (year - 1970) * 365 + ((year - 1972) / 4) - ((year - 2000) / 100) + ((year - 2000) / 400);
    }

    private int daysInMonth() {
        return daysInMonth(isLeapYear(fields[YEAR]), fields[MONTH]);
    }

    private int daysInMonth(boolean leapYear, int month) {
        if (leapYear && month == FEBRUARY) {
            return DaysInMonth[month] + 1;
        }

        return DaysInMonth[month];
    }

    private int daysInYear(int year) {
        int daysInYear = isLeapYear(year) ? 366 : 365;
        if (year == changeYear) {
            daysInYear -= currentYearSkew;
        }
        if (year == changeYear - 1) {
            daysInYear -= lastYearSkew;
        }
        return daysInYear;
    }

    private int daysInYear(boolean leapYear, int month) {
        if (leapYear && month > FEBRUARY) {
            return DaysInYear[month] + 1;
        }

        return DaysInYear[month];
    }

    /**
     * Compares the specified {@code Object} to this {@code GregorianCalendar} and returns whether
     * they are equal. To be equal, the {@code Object} must be an instance of
     * {@code GregorianCalendar} and have the same properties.
     * 
     * @param object the {@code Object} to compare with this {@code GregorianCalendar}.
     * @return {@code true} if {@code object} is equal to this {@code GregorianCalendar},
     * {@code false} otherwise.
     * @throws IllegalArgumentException if the time is not set and the time cannot be computed from
     * the current field values.
     * @see #hashCode
     */
    public boolean equals(Object object) {
        if (!(object instanceof GregorianCalendar)) {
            return false;
        }
        if (object == this) {
            return true;
        }
        return super.equals(object) && gregorianCutover == ((GregorianCalendar) object).gregorianCutover;
    }

    /**
     * Gets the maximum value of the specified field for the current date. For example, the maximum
     * number of days in the current month.
     * 
     * @param field the field.
     * @return the maximum value of the specified field.
     */
    public int getActualMaximum(int field) {
        int value;
        if ((value = maximums[field]) == leastMaximums[field]) {
            return value;
        }

        switch (field) {
        case WEEK_OF_YEAR:
        case WEEK_OF_MONTH:
            isCached = false;
            break;
        }

        complete();
        long orgTime = time;
        int result = 0;
        switch (field) {
        case WEEK_OF_YEAR:
            set(DATE, 31);
            set(MONTH, DECEMBER);
            result = get(WEEK_OF_YEAR);
            if (result == 1) {
                set(DATE, 31 - 7);
                result = get(WEEK_OF_YEAR);
            }
            areFieldsSet = false;
            break;
        case WEEK_OF_MONTH:
            set(DATE, daysInMonth());
            result = get(WEEK_OF_MONTH);
            areFieldsSet = false;
            break;
        case DATE:
            return daysInMonth();
        case DAY_OF_YEAR:
            return daysInYear(fields[YEAR]);
        case DAY_OF_WEEK_IN_MONTH:
            result = get(DAY_OF_WEEK_IN_MONTH) + ((daysInMonth() - get(DATE)) / 7);
            break;
        case YEAR:
            GregorianCalendar clone = new GregorianCalendar(time);
            if (get(ERA) == AD) {
                clone.setTimeInMillis(Long.MAX_VALUE);
            } else {
                clone.setTimeInMillis(Long.MIN_VALUE);
            }
            result = clone.get(YEAR);
            clone.set(YEAR, get(YEAR));
            if (clone.before(this)) {
                result--;
            }
            break;
        case DST_OFFSET:
            result = getMaximum(DST_OFFSET);
            break;
        }
        time = orgTime;
        return result;
    }

    /**
     * Gets the minimum value of the specified field for the current date. For the gregorian
     * calendar, this value is the same as {@code getMinimum()}.
     * 
     * @param field the field.
     * @return the minimum value of the specified field.
     */
    public int getActualMinimum(int field) {
        return getMinimum(field);
    }

    /**
     * Gets the greatest minimum value of the specified field. For the gregorian calendar, this
     * value is the same as {@code getMinimum()}.
     * 
     * @param field the field.
     * @return the greatest minimum value of the specified field.
     */
    public int getGreatestMinimum(int field) {
        return minimums[field];
    }

    /**
     * Gets the smallest maximum value of the specified field. For example, 28 for the day of month
     * field.
     * 
     * @param field the field.
     * @return the smallest maximum value of the specified field.
     */
    public int getLeastMaximum(int field) {
        // return value for WEEK_OF_YEAR should make corresponding changes when
        // the gregorian change date have been reset.
        if (gregorianCutover != defaultGregorianCutover && field == WEEK_OF_YEAR) {
            long currentTimeInMillis = time;
            setTimeInMillis(gregorianCutover);
            int actual = getActualMaximum(field);
            setTimeInMillis(currentTimeInMillis);
            return actual;
        }
        return leastMaximums[field];
    }

    /**
     * Gets the greatest maximum value of the specified field. For example, 31 for the day of month
     * field.
     * 
     * @param field the field.
     * @return the greatest maximum value of the specified field.
     */
    public int getMaximum(int field) {
        return maximums[field];
    }

    /**
     * Gets the smallest minimum value of the specified field.
     * 
     * @param field the field.
     * @return the smallest minimum value of the specified field.
     */
    public int getMinimum(int field) {
        return minimums[field];
    }

    private int getOffset(long localTime) {
        return 0;
    }

    /**
     * Returns an integer hash code for the receiver. Objects which are equal return the same value
     * for this method.
     * 
     * @return the receiver's hash.
     * 
     * @see #equals
     */
    public int hashCode() {
        return super.hashCode() + ((int) (gregorianCutover >>> 32) ^ (int) gregorianCutover);
    }

    /**
     * Returns whether the specified year is a leap year.
     * 
     * @param year the year.
     * @return {@code true} if the specified year is a leap year, {@code false} otherwise.
     */
    public boolean isLeapYear(int year) {
        if (year > changeYear) {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
        }

        return year % 4 == 0;
    }

    private int julianError() {
        return changeYear / 100 - changeYear / 400 - 2;
    }

    private int mod(int value, int mod) {
        int rem = value % mod;
        if (value < 0 && rem < 0) {
            return rem + mod;
        }
        return rem;
    }

    private int mod7(long num1) {
        int rem = (int) (num1 % 7);
        if (num1 < 0 && rem < 0) {
            return rem + 7;
        }
        return rem;
    }

    /**
     * Adds the specified amount the specified field and wraps the value of the field when it goes
     * beyond the maximum or minimum value for the current date. Other fields will be adjusted as
     * required to maintain a consistent date.
     * 
     * @param field the field to roll.
     * @param value the amount to add.
     * 
     * @throws IllegalArgumentException if an invalid field is specified.
     */
    public void roll(int field, int value) {
        if (value == 0) {
            return;
        }
        if (field < 0 || field >= ZONE_OFFSET) {
            throw new IllegalArgumentException();
        }

        isCached = false;

        complete();
        int days, day, mod, maxWeeks, newWeek;
        int max = -1;
        switch (field) {
        case YEAR:
            max = maximums[field];
            break;
        case WEEK_OF_YEAR:
            days = daysInYear(fields[YEAR]);
            day = DAY_OF_YEAR;
            mod = mod7(fields[DAY_OF_WEEK] - fields[day] - (getFirstDayOfWeek() - 1));
            maxWeeks = (days - 1 + mod) / 7 + 1;
            newWeek = mod(fields[field] - 1 + value, maxWeeks) + 1;
            if (newWeek == maxWeeks) {
                int addDays = (newWeek - fields[field]) * 7;
                if (fields[day] > addDays && fields[day] + addDays > days) {
                    set(field, 1);
                } else {
                    set(field, newWeek - 1);
                }
            } else if (newWeek == 1) {
                int week = (fields[day] - ((fields[day] - 1) / 7 * 7) - 1 + mod) / 7 + 1;
                if (week > 1) {
                    set(field, 1);
                } else {
                    set(field, newWeek);
                }
            } else {
                set(field, newWeek);
            }
            break;
        case WEEK_OF_MONTH:
            days = daysInMonth();
            day = DATE;
            mod = mod7(fields[DAY_OF_WEEK] - fields[day] - (getFirstDayOfWeek() - 1));
            maxWeeks = (days - 1 + mod) / 7 + 1;
            newWeek = mod(fields[field] - 1 + value, maxWeeks) + 1;
            if (newWeek == maxWeeks) {
                if (fields[day] + (newWeek - fields[field]) * 7 > days) {
                    set(day, days);
                } else {
                    set(field, newWeek);
                }
            } else if (newWeek == 1) {
                int week = (fields[day] - ((fields[day] - 1) / 7 * 7) - 1 + mod) / 7 + 1;
                if (week > 1) {
                    set(day, 1);
                } else {
                    set(field, newWeek);
                }
            } else {
                set(field, newWeek);
            }
            break;
        case DATE:
            max = daysInMonth();
            break;
        case DAY_OF_YEAR:
            max = daysInYear(fields[YEAR]);
            break;
        case DAY_OF_WEEK:
            max = maximums[field];
            lastDateFieldSet = WEEK_OF_MONTH;
            break;
        case DAY_OF_WEEK_IN_MONTH:
            max = (fields[DATE] + ((daysInMonth() - fields[DATE]) / 7 * 7) - 1) / 7 + 1;
            break;

        case ERA:
        case MONTH:
        case AM_PM:
        case HOUR:
        case HOUR_OF_DAY:
        case MINUTE:
        case SECOND:
        case MILLISECOND:
            set(field, mod(fields[field] + value, maximums[field] + 1));
            if (field == MONTH && fields[DATE] > daysInMonth()) {
                set(DATE, daysInMonth());
            } else if (field == AM_PM) {
                lastTimeFieldSet = HOUR;
            }
            break;
        }
        if (max != -1) {
            set(field, mod(fields[field] - 1 + value, max) + 1);
        }
        complete();
    }

    /**
     * Increments or decrements the specified field and wraps the value of the field when it goes
     * beyond the maximum or minimum value for the current date. Other fields will be adjusted as
     * required to maintain a consistent date. For example, March 31 will roll to April 30 when
     * rolling the month field.
     * 
     * @param field the field to roll.
     * @param increment {@code true} to increment the field, {@code false} to decrement.
     * @throws IllegalArgumentException if an invalid field is specified.
     */
    public void roll(int field, boolean increment) {
        roll(field, increment ? 1 : -1);
    }

    public void setTimeInMillis(long milliseconds) {
        if (!isTimeSet || !areFieldsSet || time != milliseconds) {
            time = milliseconds;
            isTimeSet = true;
            areFieldsSet = false;
            complete();
        }
    }

    /**
     * Sets a field to the specified value.
     * 
     * @param field the code indicating the {@code Calendar} field to modify.
     * @param value the value.
     */
    public void set(int field, int value) {
        fields[field] = value;
        isSet[field] = true;
        areFieldsSet = isTimeSet = false;
        if (field > MONTH && field < AM_PM) {
            lastDateFieldSet = field;
        }
        if (field == HOUR || field == HOUR_OF_DAY) {
            lastTimeFieldSet = field;
        }
        if (field == AM_PM) {
            lastTimeFieldSet = HOUR;
        }
    }

    /**
     * Gets the value of the specified field after computing the field values by calling
     * {@code complete()} first.
     * 
     * @param field the field to get.
     * @return the value of the specified field.
     * 
     * @throws IllegalArgumentException if the fields are not set, the time is not set, and the time
     * cannot be computed from the current field values.
     * @throws ArrayIndexOutOfBoundsException if the field is not inside the range of possible
     * fields. The range is starting at 0 up to {@code FIELD_COUNT}.
     */
    public int get(int field) {
        complete();
        return fields[field];
    }

    /**
     * Returns whether the specified field is set. Note that the interpretation of "is set" is
     * somewhat technical. In particular, it does <i>not</i> mean that the field's value is up to
     * date. If you want to know whether a field contains an up-to-date value, you must also check
     * {@code areFieldsSet}, making this method somewhat useless unless you're a subclass, in which
     * case you can access the {@code isSet} array directly.
     * <p>
     * A field remains "set" from the first time its value is computed until it's cleared by one of
     * the {@code clear} methods. Thus "set" does not mean "valid". You probably want to call
     * {@code get} -- which will update fields as necessary -- rather than try to make use of this
     * method.
     * 
     * @param field a {@code Calendar} field number.
     * @return {@code true} if the specified field is set, {@code false} otherwise.
     */
    public final boolean isSet(int field) {
        return isSet[field];
    }

    /**
     * Clears the specified field to zero and sets the isSet flag to {@code false}.
     * 
     * @param field the field to clear.
     */
    public final void clear(int field) {
        fields[field] = 0;
        isSet[field] = false;
        areFieldsSet = isTimeSet = false;
    }

    /**
     * Returns if this {@code Calendar} accepts field values which are outside the valid range for
     * the field.
     * 
     * @return {@code true} if this {@code Calendar} is lenient, {@code false} otherwise.
     */
    public boolean isLenient() {
        return lenient;
    }

    /**
     * Sets this {@code Calendar} to accept field values which are outside the valid range for the
     * field.
     * 
     * @param value a boolean value.
     */
    public void setLenient(boolean value) {
        lenient = value;
    }

    /**
     * Gets the first day of the week for this {@code Calendar}.
     * 
     * @return the first day of the week.
     */
    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /**
     * Gets the minimal days in the first week of the year.
     * 
     * @return the minimal days in the first week of the year.
     */
    public int getMinimalDaysInFirstWeek() {
        return minimalDaysInFirstWeek;
    }

    /**
     * Computes the time from the fields if the time has not already been set. Computes the fields
     * from the time if the fields are not already set.
     * 
     * @throws IllegalArgumentException if the time is not set and the time cannot be computed from
     * the current field values.
     */
    protected void complete() {
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
        }
        if (!areFieldsSet) {
            computeFields();
            areFieldsSet = true;
        }
    }

    /**
     * Returns whether the {@code Date} specified by this {@code Calendar} instance is before the
     * {@code Date} specified by the parameter. The comparison is not dependent on the time zones of
     * the {@code Calendar}.
     * 
     * @param calendar the {@code Calendar} instance to compare.
     * @return {@code true} when this Calendar is before calendar, {@code false} otherwise.
     * @throws IllegalArgumentException if the time is not set and the time cannot be computed from
     * the current field values.
     */
    public boolean before(Object calendar) {
        if (!(calendar instanceof GregorianCalendar)) {
            return false;
        }
        return getTimeInMillis() < ((GregorianCalendar) calendar).getTimeInMillis();
    }

    /**
     * Computes the time from the fields if required and returns the time.
     * 
     * @return the time of this {@code Calendar}.
     * 
     * @throws IllegalArgumentException if the time is not set and the time cannot be computed from
     * the current field values.
     */
    public long getTimeInMillis() {
        if (!isTimeSet) {
            computeTime();
            isTimeSet = true;
        }
        return time;
    }

    /**
     * Sets the first day of the week for this {@code Calendar}.
     * 
     * @param value a {@code Calendar} day of the week.
     */
    public void setFirstDayOfWeek(int value) {
        firstDayOfWeek = value;
        isCached = false;
    }

    /**
     * Sets the minimal days in the first week of the year.
     * 
     * @param value the minimal days in the first week of the year.
     */
    public void setMinimalDaysInFirstWeek(int value) {
        minimalDaysInFirstWeek = value;
        isCached = false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (fields[MONTH] + 1 <= 9) {
            sb.append('0');
        }
        sb.append(fields[MONTH] + 1);
        sb.append('-');
        if (fields[DAY_OF_MONTH] <= 9) {
            sb.append('0');
        }
        sb.append(fields[DAY_OF_MONTH]);
        sb.append(' ');
        if (fields[HOUR_OF_DAY] <= 9) {
            sb.append('0');
        }
        sb.append(fields[HOUR_OF_DAY]);
        sb.append(':');
        if (fields[MINUTE] <= 9) {
            sb.append('0');
        }
        sb.append(fields[MINUTE]);
        sb.append(':');
        if (fields[SECOND] <= 9) {
            sb.append('0');
        }
        sb.append(fields[SECOND]);
        sb.append('.');
        if (fields[MILLISECOND] <= 9) {
            sb.append('0');
        }
        if (fields[MILLISECOND] <= 99) {
            sb.append('0');
        }
        sb.append(fields[MILLISECOND]);
        return sb.toString();
    }
}
