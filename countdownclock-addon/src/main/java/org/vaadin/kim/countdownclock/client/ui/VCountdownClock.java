package org.vaadin.kim.countdownclock.client.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class VCountdownClock extends Widget {

	public interface CountdownEndedListener {
		public void countdownEnded();
	}

	public static enum Direction {
		UP, DOWN
	}

	/** Set the tagname used to statically resolve widget from UIDL. */
	public static final String TAGNAME = "countdownclock";

	/** Set the CSS class name to allow styling. */
	public static final String CLASSNAME = "v-" + TAGNAME;

	public static final String CLASSNAME_OVERTIME = CLASSNAME + "-overtime";

	private long lastTimerTick;
	private long time = 0;
	private Long endTime;
	private boolean overtime = false;

	protected Counter counter = new Counter();

	protected List<TimeString> formatStrings = new ArrayList<TimeString>();

	protected String formatPrefix = "";

	protected List<TimeType> formatsPresent = new ArrayList<TimeType>();

	protected List<CountdownEndedListener> listeners = new ArrayList<VCountdownClock.CountdownEndedListener>();

	// seconds, minutes, hours, day
	protected int oneDay = 1000 * 60 * 60 * 24;
	// seconds, minutes, hours
	protected int anHour = 1000 * 60 * 60;
	// seconds, minutes
	protected int aMinute = 1000 * 60;
	// second
	protected int aSecond = 1000;

	protected Direction direction = Direction.DOWN;
	protected int timerInterval = 1000;

	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VCountdownClock() {
		setElement(Document.get().createDivElement());
		// This method call of the Paintable interface sets the component
		// style name in DOM tree
		setStyleName(CLASSNAME);
	}

	/**
	 * Neglect higher units; so if we have 2 days and 3 hours left that would mean
	 * 27 hours. Set to true if you want %h to return 3 and not 27. Note %h would
	 * return 3 anyway in case a %d is detected in the format string.
	 *
	 * @param neglect
	 *            Whether or not to neglect higher units.
	 */
	protected void setNeglectHigherUnits(boolean neglect) {
		this.neglectHigher = neglect;
	}

	private boolean neglectHigher = false;

	private boolean continueAfterEnd = false;

	protected void setTimeFormat(String format) {

		formatsPresent.clear();
		formatStrings.clear();
		formatPrefix = "";

		// Create the format
		while (format.length() > 0) {
			int pos = format.indexOf("%");
			if (pos >= 0) {
				String before = pos > 0 ? format.substring(0, pos) : "";

				format = format.substring(pos);
				String type = format.substring(0, 2);
				int removeChars = 2;
				if (type.equals("%-")) {
					TimeString ts = new TimeString(TimeType.SIGN);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.SIGN);
				} else if (type.equals("%d")) {
					TimeString ts = new TimeString(TimeType.DAYS);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.DAYS);
				} else if (type.equals("%h")) {
					TimeString ts = new TimeString(TimeType.HOURS);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.HOURS);
				} else if (type.equals("%m")) {
					TimeString ts = new TimeString(TimeType.MINUTES);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.MINUTES);
				} else if (type.equals("%s")) {
					TimeString ts = new TimeString(TimeType.SECONDS);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.SECONDS);
				} else if (type.equals("%M")) {
					TimeString ts = new TimeString(TimeType.MINUTES_TWO_DIGIT);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.MINUTES_TWO_DIGIT);
				} else if (type.equals("%S")) {
					TimeString ts = new TimeString(TimeType.SECONDS_TWO_DIGIT);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.SECONDS_TWO_DIGIT);
				} else if (format.substring(0, 3).equals("%ts")) {
					TimeString ts = new TimeString(TimeType.TENTH_OF_A_SECONDS);
					formatStrings.add(ts);
					formatsPresent.add(TimeType.TENTH_OF_A_SECONDS);
					removeChars = 3;
				} else {
					before += type;
				}

				if (formatStrings.size() <= 1) {
					formatPrefix = before;
				} else {
					formatStrings.get(formatStrings.size() - 2).setPostfix(before);
				}

				format = format.substring(removeChars);
			} else {
				if (formatStrings.size() < 1) {
					formatPrefix = format;
				} else {
					formatStrings.get(formatStrings.size() - 1).setPostfix(format);
				}
				format = "";
			}
		}

		if (formatsPresent.contains(TimeType.TENTH_OF_A_SECONDS)) {
			timerInterval = 100;
		} else if (formatsPresent.contains(TimeType.SECONDS)) {
			timerInterval = aSecond;
		} else if (formatsPresent.contains(TimeType.MINUTES)) {
			timerInterval = aMinute;
		} else if (formatsPresent.contains(TimeType.HOURS)) {
			timerInterval = anHour;
		} else if (formatsPresent.contains(TimeType.DAYS)) {
			timerInterval = oneDay;
		}
	}

	private String format(long time) {
		if (time < 0) {
			time = time * -1;
		}

		String str = "";
		if (formatPrefix != null) {
			str += formatPrefix;
		}

		for (TimeString ts : formatStrings) {
			str += ts.getValue(time);
		}
		return str;
	}

	public void startClock() {
		counter.scheduleRepeating(timerInterval);
		lastTimerTick = new Date().getTime();
		overtime = false;
		counter.run();
	}

	protected void updateLabel() {
		getElement().setInnerHTML(format(getTime()));
	}

	protected class Counter extends Timer {
		@Override
		public void run() {
			// can't trust the timer precision
			long elapsedMillis = new Date().getTime() - lastTimerTick;
			lastTimerTick = new Date().getTime();

			setTime(getTime() + ((direction == Direction.UP ? 1 : -1) * elapsedMillis));

			if (getEndTime() != null && ((direction == Direction.UP && getTime() >= getEndTime())
					|| (direction == Direction.DOWN && getTime() <= getEndTime()))) {
				// overtime
				if (overtime == false) {
					fireEndEvent();
				}
				if (getContinueAfterEnd() == false) {
					cancel();
					setTime(getEndTime());
					return;
				} else {
					if (overtime == false) {
						addStyleName(CLASSNAME_OVERTIME);
					}
				}
				if (overtime == false) {
					overtime = true;
				}
			} else {
				removeStyleName(CLASSNAME_OVERTIME);
			}
			updateLabel();
		}
	}

	protected class TimeString {

		protected String postfix = "";

		protected TimeType type = null;

		public TimeString(TimeType type) {
			this.type = type;
		}

		public void setPostfix(String postfix) {
			this.postfix = postfix;
		}

		public String getPostfix() {
			return postfix;
		}

		private String leftPadWithZeroes(long number, int digits) {
			String string = number + "";
			while(string.length() < digits) {
				string = "0" + string;
			}
			return string;
		}
		
		public String getValue(long milliseconds) {
			boolean negative = milliseconds < 0;
			milliseconds = negative ? milliseconds * -1 : milliseconds;

			if (type.equals(TimeType.SIGN)) {
				return negative ? "-" : "";
			} else if (type.equals(TimeType.DAYS)) {
				return getDays(milliseconds) + postfix;
			} else if (type.equals(TimeType.HOURS)) {
				// Check if a day exists in the format, in that case remove all
				// full days from the time
				if (neglectHigher || formatsPresent.contains(TimeType.DAYS)) {
					milliseconds -= getDays(milliseconds) * oneDay;
				}
				return getHours(milliseconds) + postfix;
			} else if (type.equals(TimeType.MINUTES) || type.equals(TimeType.MINUTES_TWO_DIGIT)) {
				// Check if a day exists in the format, in that case remove all
				// full days from the time
				if (neglectHigher || formatsPresent.contains(TimeType.DAYS)) {
					milliseconds -= getDays(milliseconds) * oneDay;
				}
				if (neglectHigher || formatsPresent.contains(TimeType.HOURS)) {
					milliseconds -= getHours(milliseconds) * anHour;
				}
				if (type.equals(TimeType.MINUTES_TWO_DIGIT)) {
					return leftPadWithZeroes(getMinutes(milliseconds), 2) + postfix;
				} else {
					return getMinutes(milliseconds) + postfix;
				}
			} else if (type.equals(TimeType.SECONDS) || type.equals(TimeType.SECONDS_TWO_DIGIT)) {
				// Check if a day exists in the format, in that case remove all
				// full days from the time
				if (neglectHigher || formatsPresent.contains(TimeType.DAYS)) {
					milliseconds -= getDays(milliseconds) * oneDay;
				}
				if (neglectHigher || formatsPresent.contains(TimeType.HOURS)) {
					milliseconds -= getHours(milliseconds) * anHour;
				}
				if (neglectHigher || formatsPresent.contains(TimeType.MINUTES) || formatsPresent.contains(TimeType.MINUTES_TWO_DIGIT)) {
					milliseconds -= getMinutes(milliseconds) * aMinute;
				}
				if (type.equals(TimeType.SECONDS_TWO_DIGIT)) {
					return leftPadWithZeroes(getSeconds(milliseconds), 2) + postfix;
				} else {
					return getSeconds(milliseconds) + postfix;
				}
			} else if (type.equals(TimeType.TENTH_OF_A_SECONDS)) {
				// Check if a day exists in the format, in that case remove all
				// full days from the time
				if (neglectHigher || formatsPresent.contains(TimeType.DAYS)) {
					milliseconds -= getDays(milliseconds) * oneDay;
				}
				if (neglectHigher || formatsPresent.contains(TimeType.HOURS)) {
					milliseconds -= getHours(milliseconds) * anHour;
				}
				if (neglectHigher || formatsPresent.contains(TimeType.MINUTES)) {
					milliseconds -= getMinutes(milliseconds) * aMinute;
				}
				if (neglectHigher || formatsPresent.contains(TimeType.SECONDS)) {
					milliseconds -= getSeconds(milliseconds) * aSecond;
				}
				return Math.round(milliseconds / 100) + postfix;
			} else {
				return "";
			}

		}

		@Override
		public String toString() {
			return type.name();
		}

		protected long getDays(long milliseconds) {
			return (long) Math.floor(milliseconds / oneDay);
		}

		protected long getHours(long milliseconds) {
			return (long) Math.floor(milliseconds / anHour);
		}

		protected long getMinutes(long milliseconds) {
			return (long) Math.floor(milliseconds / aMinute);
		}

		protected long getSeconds(long milliseconds) {
			return (long) Math.floor(milliseconds / aSecond);
		}
	}

	protected enum TimeType {
		DAYS, HOURS, MINUTES, SECONDS, MINUTES_TWO_DIGIT, SECONDS_TWO_DIGIT, TENTH_OF_A_SECONDS, SIGN
	};

	@Override
	protected void onDetach() {
		super.onDetach();
		counter.cancel();
	}

	public void fireEndEvent() {
		for (CountdownEndedListener listener : listeners) {
			listener.countdownEnded();
		}
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long counterTarget) {
		this.endTime = counterTarget;
	}

	public boolean getContinueAfterEnd() {
		return continueAfterEnd;
	}

	public void addListener(CountdownEndedListener listener) {
		listeners.add(listener);
	}

	public void setContinueAfterEnd(boolean continueAfterEnd) {
		this.continueAfterEnd = continueAfterEnd;
	}

	public void stop() {
		counter.cancel();
	}
}
