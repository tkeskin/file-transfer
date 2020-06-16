package tr.com.aa.util;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class MyDateUtils {

  //private static final Logger LOGGER = LoggerFactory.getLogger(MyDateUtils.class);

  public static String yearPatternReg = "((19|20)[0-9]{2})";

  public static String datePatternReg =
      "((19|20)[0-9]{2}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01]))";

  public static String time24PatternReg =
      "((0[0-9]|1[0-9]|2[0-4]):(0[0-9]|[1-5][0-9]|60):(0[0-9]|[1-5][0-9]|60))";

  public static String dateTime24PatternReg = datePatternReg + " " + time24PatternReg;

  // year
  public static String yearPattern = "yyyy";
  // date
  // public static String datePattern = "yyyy-MM-dd";
  // date
  public static String datePattern = "yyyy-MM-dd";

  public static String timePattern24 = "HH:mm:ss";

  public static String dateTime24Pattern = datePattern + " " + timePattern24;

  public static String timePattern12 = "hh:mm:ss";

  public static String dateTime12Pattern = datePattern + " " + timePattern12;

  private MyDateUtils() {
    // no construct function
  }

  /**
   * @param date .
   * @return .
   */
  public static LocalDate asLocalDate(Date date) {

    return asLocalDateTime(date).toLocalDate();
  }

  /**
   * @param date .
   * @return .
   */
  public static LocalTime asLocalTime(Date date) {

    return asLocalDateTime(date).toLocalTime();
  }

  /**
   * @param date .
   * @return .
   */
  public static LocalDateTime asLocalDateTime(Date date) {

    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  /**
   * @param milliseconds .
   * @return .
   */
  public static LocalDateTime asLocalDateTime(long milliseconds) {

    return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
  }

  /**
   * @param localDateTime .
   * @return .
   */
  public static Long asLong(LocalDateTime localDateTime) {

    ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
    return zdt.toInstant().toEpochMilli();
  }

  /**
   * @param date .
   * @return .
   */
  public static Date asDate(LocalDate date) {

    Instant instant = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
    return Date.from(instant);
  }

  /**
   * @param date .
   * @return .
   */
  public static Date astDate(LocalTime date) {

    Instant instant = date.atDate(LocalDate
        .of(LocalDate.now().getYear(), LocalDate.now().getMonth(), LocalDate.now().getDayOfMonth()))
        .atZone(ZoneId.systemDefault()).toInstant();
    return Date.from(instant);
  }

  /**
   * @param date .
   * @return .
   */
  public static Date asDateTime(LocalDateTime date) {

    Instant instant = date.atZone(ZoneId.systemDefault()).toInstant();
    return Date.from(instant);
  }

  public static LocalDateTime parseLocalDateTime(String localDateTimeStr, String format) {

    return LocalDateTime.parse(localDateTimeStr, DateTimeFormatter.ofPattern(format));
  }

  public static String parseLocalDateTime(LocalDateTime localDateTime, String format) {

    return localDateTime.format(DateTimeFormatter.ofPattern(format));
  }

  private void examples() {

    LocalDate today = LocalDate.now();
    System.out.println("Today's Local date : " + today);

    int year = today.getYear();
    int month = today.getMonthValue();
    int day = today.getDayOfMonth();
    System.out.printf("Year : %d Month : %d day : %d \t %n", year, month, day);

    LocalDate dateOfBirth = LocalDate.of(2010, 01, 14);
    System.out.println("Your Date of birth is : " + dateOfBirth);

    LocalDate date1 = LocalDate.of(2014, 01, 14);
    if (date1.equals(today)) {
      System.out.printf("Today %s and date1 %s are same date %n", today, date1);
    }

    MonthDay birthday = MonthDay.of(dateOfBirth.getMonth(), dateOfBirth.getDayOfMonth());
    MonthDay currentMonthDay = MonthDay.from(today);
    if (currentMonthDay.equals(birthday)) {
      System.out.println("Many Many happy returns of the day !!");
    } else {
      System.out.println("Sorry, today is not your birthday");
    }

    LocalTime time = LocalTime.now();
    LocalTime newTime = time.plusHours(2); // adding two hours
    System.out.println("Time after 2 hours : " + newTime);

    LocalDate nextWeek = today.plus(1, ChronoUnit.WEEKS);
    System.out.println("Today is : " + today);
    System.out.println("Date after 1 week : " + nextWeek);

    LocalDate previousYear = today.minus(1, ChronoUnit.YEARS);
    System.out.println("Date before 1 year : " + previousYear);
    LocalDate nextYear = today.plus(1, ChronoUnit.YEARS);
    System.out.println("Date after 1 year : " + nextYear);

    Clock clock = Clock.systemUTC();
    System.out.println("Clock : " + clock);

    // Returns time based on system clock zone Clock defaultClock =
    Clock.systemDefaultZone();
    System.out.println("Clock : " + clock);

    LocalDate tomorrow = LocalDate.of(2014, 1, 15);
    if (tomorrow.isAfter(today)) {
      System.out.println("Tomorrow comes after today");
    }
    LocalDate yesterday = today.minus(1, ChronoUnit.DAYS);
    if (yesterday.isBefore(today)) {
      System.out.println("Yesterday is day before today");
    }

    LocalDateTime localtDateAndTime = LocalDateTime.now();
    ZoneId america = ZoneId.of("America/New_York");
    ZonedDateTime dateAndTimeInNewYork = ZonedDateTime.of(localtDateAndTime, america);
    System.out.println("Current date and time in a particular timezone : " + dateAndTimeInNewYork);
    YearMonth currentYearMonth = YearMonth.now();
    System.out
        .printf("Days in month year %s: %d%n", currentYearMonth, currentYearMonth.lengthOfMonth());
    YearMonth creditCardExpiry = YearMonth.of(2018, Month.FEBRUARY);
    System.out.printf("Your credit card expires on %s %n", creditCardExpiry);

    if (today.isLeapYear()) {
      System.out.println("This year is Leap year");
    } else {
      System.out.println("2014 is not a Leap year");
    }

    LocalDate java8Release = LocalDate.of(2014, Month.MARCH, 14);
    Period periodToNextJavaRelease =
        Period.between(today, java8Release);
    System.out.println(
        "Months left between today and Java 8 release : " + periodToNextJavaRelease.getMonths());

    LocalDateTime datetime = LocalDateTime.of(2014, Month.JANUARY, 14, 19, 30);
    ZoneOffset offset = ZoneOffset.of("+05:30");
    OffsetDateTime date = OffsetDateTime.of(datetime, offset);
    System.out.println("Date and Time with timezone offset in Java : " + date);

    Instant timestamp = Instant.now();
    System.out.println("What is value of this instant " + timestamp);

    String dayAfterTommorrow = "20140116";
    LocalDate formatted = LocalDate.parse(dayAfterTommorrow, DateTimeFormatter.BASIC_ISO_DATE);
    System.out.printf("Date generated from String %s is %s %n", dayAfterTommorrow, formatted);

    String goodFriday = "Apr 18 2014";
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd yyyy");
      LocalDate holiday = LocalDate.parse(goodFriday, formatter);
      System.out.printf("Successfully parsed String %s, date is %s%n", goodFriday, holiday);
    } catch (DateTimeParseException ex) {
      System.out.printf("%s is not parsable!%n", goodFriday);
      ex.printStackTrace();
    }

    LocalDateTime arrivalDate = LocalDateTime.now();
    try {
      DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM dd yyyy hh:mm a");
      String landing = arrivalDate.format(format);
      System.out.printf("Arriving at : %s %n", landing);
    } catch (DateTimeException ex) {
      System.out.printf("%s can't be formatted!%n", arrivalDate);
      ex.printStackTrace();
    }
  }
}
