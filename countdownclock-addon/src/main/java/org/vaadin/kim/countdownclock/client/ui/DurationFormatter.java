package org.vaadin.kim.countdownclock.client.ui;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class DurationFormatter {

	private static final int MILLIS = 1;
	private static final int THENTH_OF_SECONDS = 100 * MILLIS;
	private static final int SECONDS = 1000 * MILLIS;
	private static final int MINUTES = 60 * SECONDS;
	private static final int HOURS = 60 * MINUTES;
	private static final int DAYS = 24 * HOURS;

	private static Integer minPrecision(List<Formatter> formatters) {
		int min = Integer.MAX_VALUE;
		for (Formatter f : formatters) {
			Integer p = f.getPrecision();
			if (p != null && p < min) {
				min = p;
			}
		}
		return min != Integer.MAX_VALUE ? min : null;
	}

	private interface Formatter {
		String format(long millis);

		Integer getPrecision();
	}

	private class SignFormatter implements Formatter {
		private boolean lazy;

		public SignFormatter(boolean lazy) {
			super();
			this.lazy = lazy;
		}

		@Override
		public String format(long millis) {
			return millis < 0 ? "-" : lazy ? "+" : "";
		}

		@Override
		public Integer getPrecision() {
			return null;
		}
	}

	private abstract class TimeFormatter implements Formatter {

		private final int digitsNumber;
		private int precision;

		public TimeFormatter(int precision, int digits) {
			this.digitsNumber = digits;
			this.precision = precision;
		}

		protected boolean printSign() {
			for (Formatter f : DurationFormatter.this.formatters) {
				if (f instanceof SignFormatter) {
					return false;
				}
			}
			return true;
		}

		protected abstract long getValue(long millis);

		@Override
		public String format(long millis) {
			return format(getValue(millis), digitsNumber);
		}

		private String format(long value, int digits) {
			String sign = "";
			if (value < 0) {
				sign = "-";
				value = value * -1;
			}
			String digitsString = Long.toString(value);
			while (digitsString.length() < digits) {
				digitsString = "0" + digitsString;
			}
			return (printSign() ? "" : sign) + digitsString;
		}

		@Override
		public Integer getPrecision() {
			return precision;
		}
	}

	private class StringFormatter implements Formatter {

		private String string;

		public StringFormatter(String string) {
			this.string = string;
		}

		public String getString() {
			return string;
		}

		@Override
		public String format(long value) {
			return string;
		}

		@Override
		public Integer getPrecision() {
			return null;
		}

	}

	private class JavaScriptFormatter implements Formatter {

		private List<Formatter> formatters;

		public JavaScriptFormatter(List<Formatter> formatters) {
			this.formatters = formatters;
		}

		@Override
		public String format(long millis) {
			StringBuilder sb = new StringBuilder();
			for (Formatter f : formatters) {
				sb.append(f.format(millis));
			}
			String formattedJs = sb.toString();
			return eval(formattedJs);
		}

		public native String eval(String arg) /*-{
												return eval(arg);
												}-*/;

		public Integer getPrecision() {
			return minPrecision(formatters);
		};

	}

	private abstract class TockenMatcher {
		public abstract List<Formatter> compile(String format);
	}

	private abstract class RegexMatcher extends TockenMatcher {

		private String tokenRegex;

		public RegexMatcher(String tokenRegex) {
			this.tokenRegex = tokenRegex;
		}

		@Override
		public List<Formatter> compile(String format) {
			LinkedList<Formatter> list = new LinkedList<>();
			RegExp pattern = RegExp.compile(tokenRegex, "g");
			MatchResult match = null;
			MatchResult previousMatch = null;
			while ((match = pattern.exec(format)) != null) {
				if (previousMatch != null) {
					String s = format.substring(previousMatch.getIndex() + previousMatch.getGroup(0).length(),
							match.getIndex());
					if (!s.isEmpty()) {
						list.add(new StringFormatter(s));
					}
				} else if (match.getIndex() > 0) {
					list.add(new StringFormatter(format.substring(0, match.getIndex())));
				}
				list.add(buildFormatter(match));

				previousMatch = match;
			}
			if (previousMatch != null) {
				list.add(new StringFormatter(
						format.substring(previousMatch.getIndex() + previousMatch.getGroup(0).length())));
			}
			if (!list.isEmpty()) {
				return list;
			} else {
				return null;
			}
		}

		protected abstract Formatter buildFormatter(MatchResult match);

	}

	private List<TockenMatcher> builders = new LinkedList<>();
	private List<Formatter> formatters;

	/**
	 * Set the format. Available parameters:
	 * 
	 * <ul>
	 * <li>%sign minus sign if the time is negative or empty string (all other value will be without sign if present)</li>
	 * <li>%SIGN minus or plus sign (all other value will be without sign if present)</li>
	 * <li>%nosign no sign even if negative (all other value will be without sign if present)</li>
	 * <li></li>
	 * <li>%d days</li>
	 * <li></li>
	 * <li>%h hours of day (reset at 23)</li>
	 * <li>%hh hours of day, two digits</li>
	 * <li>%H hours total</li>
	 * <li></li>
	 * <li>%m minutes of hour</li>
	 * <li>%mm minutes of hour, two digits</li>
	 * <li>%M minutes total</li>
	 * <li></li>
	 * <li>%s seconds fo minute</li>
	 * <li>%ss seconds of minute, two digits</li>
	 * <li>%S seconds total</li>
	 * <li></li>
	 * <li>%ts tenth of a seconds</li>
	 * </ul>
	 * 
	 * For example "%d day(s) %h hour(s) and %m minutes" could produce the string "2
	 * day(s) 23 hour(s) and 5 minutes"
	 */
	public DurationFormatter(String format) {

		builders.add(new TockenMatcher() {
			private static final String START_TOKEN = "%js{";

			@Override
			public List<Formatter> compile(String format) {
				List<Formatter> list = new LinkedList<>();

				int startIndex = format.indexOf(START_TOKEN);
				if (startIndex >= 0) {
					list.add(new StringFormatter(format.substring(0, startIndex)));

					startIndex += START_TOKEN.length();

					String js = findOuterMatchingTockensContent(format.substring(startIndex - 1), "{", "}");
					if (js != null) {
						list.add(new JavaScriptFormatter(DurationFormatter.this.compile(js)));
					} else {
						list.add(new StringFormatter("!!formato errato!!"));
					}
				}

				if (!list.isEmpty()) {
					return list;
				} else {
					return null;
				}
			}

			private String findOuterMatchingTockensContent(String string, String startToken, String endToken) {
				int startIndex = -1;
				int endIndex = -1;

				int nestedPairs = 0;
				for (int i = 0; i < string.length(); i++) {

					if (string.startsWith(startToken, i)) {
						if (nestedPairs == 0) {
							startIndex = i;
						}
						nestedPairs = nestedPairs + 1;
						i += startToken.length() - 1;
					} else if (string.startsWith(endToken, i)) {
						nestedPairs = nestedPairs - 1;
						if (nestedPairs == 0) {
							endIndex = i;
						}
						i += endToken.length() - 1;
					}
				}
				if (startIndex >= 0 && endIndex > 0) {
					return string.substring(startIndex + 1, endIndex);
				} else {
					return null;
				}
			}
		});
		builders.add(new RegexMatcher("%nosign|%sign|%SIGN") {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				if (match.getGroup(0).equals("%nosign")) {
					return new StringFormatter("");
				} else {
					return new SignFormatter(match.getGroup(0).equals("%sign"));
				}
			}
		});
		builders.add(new RegexMatcher("%tts|%TTS|%ts|%TS") {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (match.getGroup(0).equals("%tts") || match.getGroup(0).equals("%TTS")) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (match.getGroup(0).equals("%ts") || match.getGroup(0).equals("%tts")) {
					return new TimeFormatter(THENTH_OF_SECONDS, digits) {
						@Override
						protected long getValue(long millis) {
							return millis % SECONDS / THENTH_OF_SECONDS;
						}
					};
				} else {
					return new TimeFormatter(THENTH_OF_SECONDS, digits) {
						@Override
						protected long getValue(long millis) {
							return millis / THENTH_OF_SECONDS;
						}
					};

				}
			}
		});
		builders.add(new RegexMatcher("%ss|%SS|%s|%S") {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (match.getGroup(0).equals("%ss") || match.getGroup(0).equals("%SS")) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (match.getGroup(0).equals("%s") || match.getGroup(0).equals("%ss")) {
					return new TimeFormatter(SECONDS, digits) {
						@Override
						protected long getValue(long millis) {
							return millis % MINUTES / SECONDS;
						}
					};
				} else {
					return new TimeFormatter(SECONDS, digits) {
						@Override
						protected long getValue(long millis) {
							return millis / SECONDS;
						}
					};
				}
			}
		});
		builders.add(new RegexMatcher("%mm|%MM|%m|%M") {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (match.getGroup(0).equals("%mm") || match.getGroup(0).equals("%MM")) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (match.getGroup(0).equals("%m") || match.getGroup(0).equals("%mm")) {
					return new TimeFormatter(MINUTES, digits) {
						@Override
						protected long getValue(long millis) {
							return millis % HOURS / MINUTES;
						}
					};
				} else {
					return new TimeFormatter(MINUTES, digits) {
						@Override
						protected long getValue(long millis) {
							return millis / MINUTES;
						}
					};
				}
			}
		});
		builders.add(new RegexMatcher("%hh|%HH|%h|%H") {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (match.getGroup(0).equals("%hh") || match.getGroup(0).equals("%HH")) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (match.getGroup(0).equals("%h") || match.getGroup(0).equals("%hh")) {
					return new TimeFormatter(HOURS, digits) {
						@Override
						protected long getValue(long millis) {
							return millis % DAYS / HOURS;
						}
					};
				} else {
					return new TimeFormatter(HOURS, digits) {
						@Override
						protected long getValue(long millis) {
							return millis / HOURS;
						}
					};
				}
			}
		});
		builders.add(new RegexMatcher("%d") {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				return new TimeFormatter(DAYS, 1) {
					@Override
					protected long getValue(long millis) {
						return millis / DAYS;
					}
				};
			}
		});

		formatters = compile(format);
	}

	protected List<Formatter> compile(String format) {
		for (TockenMatcher pbuilder : builders) {
			List<Formatter> formatters = pbuilder.compile(format);
			if (formatters != null) {
				for (int i = 0; i < formatters.size(); i++) {
					Formatter f = formatters.get(i);
					if (f instanceof StringFormatter) {
						List<Formatter> subFormatters = compile(((StringFormatter) f).getString());
						if (subFormatters != null) {
							formatters.remove(i);
							formatters.addAll(i, subFormatters);
						}
					}
				}
				return formatters;
			}
		}
		return null;
	}

	public String format(long millis) {
		StringBuilder sb = new StringBuilder();
		for (Formatter f : formatters) {
			sb.append(f.format(millis));
		}
		return sb.toString();
	}

	public Integer getSmallestUsedPrecision() {
		return minPrecision(formatters);
	}

}
