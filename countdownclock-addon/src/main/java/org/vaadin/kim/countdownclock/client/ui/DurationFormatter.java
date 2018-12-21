package org.vaadin.kim.countdownclock.client.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class DurationFormatter {

	private static final int MILLIS = 1;
	private static final int THENTH_OF_SECONDS = 100 * MILLIS;
	private static final int SECONDS = 1000 * MILLIS;
	private static final int MINUTES = 60 * SECONDS;
	private static final int HOURS = 60 * MINUTES;
	private static final int DAYS = 24 * HOURS;

	private static <T> boolean in(T value, T... values) {
		for (T v : values) {
			if (Objects.equals(v, value)) {
				return true;
			}
		}
		return false;
	}

	private static String join(String separator, Collection<String> strings) {
		return join(separator, strings.toArray(new String[strings.size()]));
	}

	private static String join(String separator, String... strings) {
		StringBuilder sb = new StringBuilder();
		if (strings.length >= 1) {
			sb.append(strings[0]);
			for (int i = 1; i < strings.length; i++) {
				sb.append(separator + strings[i]);
			}
		}
		return sb.toString();
	}

	private static String altRx(String string) {
		List<String> res = new LinkedList<>();
		for (String s : string.split("\\|")) {
			for (String ss : alt(new String[] { s })) {
				res.add(ss.replace("{", "\\{").replaceAll("}", "\\}"));
			}
		}
		return join("|", res);
	}

	private static String[] alt(String... strings) {
		List<String> ret = new LinkedList<>();
		for (String s : strings) {
			ret.add(s);
			if (s.startsWith("%")) {
				ret.add("%{" + s.substring(1) + "}");
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

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

	private static Matcher match(String input, Pattern pattern) {
		return match(input, pattern, 0);
	}

	private static Matcher match(String input, Pattern pattern, int index) {
		Matcher matcher = pattern.matcher(input);
		if (matcher.find(index)) {
			return matcher;
		} else {
			return null;
		}
	}

	private static class SingleMatchResult implements java.util.regex.MatchResult {

		private final String string;
		private final int start;
		private final int end;
		private final int groupStart;
		private final int groupEnd;

		public SingleMatchResult(String string, int start, int groupStart, int groupEnd, int end) {
			super();
			this.string = string;
			this.start = start;
			this.end = end;
			this.groupStart = groupStart;
			this.groupEnd = groupEnd;
		}

		@Override
		public int start() {
			return start;
		}

		@Override
		public int start(int group) {
			assert group == 0;
			return groupStart;
		}

		@Override
		public int end() {
			return end;
		}

		@Override
		public int end(int group) {
			assert group == 0;
			return groupEnd;
		}

		@Override
		public String group() {
			return string;
		}

		@Override
		public String group(int group) {
			if (group == 0) {
				return string;
			} else {
				return string.substring(groupStart, groupEnd);
			}
		}

		@Override
		public int groupCount() {
			return 1;
		}

	}

	private static SingleMatchResult findOuterMatchingTockensContent(String string, Pattern startToken,
			Pattern endToken) {
		int start = -1;
		int gStart = -1;
		int gEnd = -1;
		int end = -1;

		int nestedPairs = 0;
		for (int i = 0; i < string.length(); i++) {
			Matcher matcher;
			// startwith
			if ((matcher = match(string, startToken, i)) != null && matcher.start() == i) {
				if (nestedPairs == 0) {
					start = i;
					gStart = matcher.end(1);
				}
				nestedPairs = nestedPairs + 1;
				i += matcher.end() - matcher.start() - 1;
			} else if ((matcher = match(string, endToken, i)) != null && matcher.start() == i) {
				nestedPairs = nestedPairs - 1;
				if (nestedPairs == 0) {
					end = matcher.end(1);
					gEnd = matcher.start(1);
					break;
				}
				i += matcher.end() - matcher.start() - 1;
			}
		}
		if (start >= 0 && end > 0) {
			return new SingleMatchResult(string, start, gStart, gEnd, end);
		} else {
			return null;
		}
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

	private static class StringFormatter implements Formatter {

		private String string;

		public StringFormatter(String string) {
			assert string != null && !string.isEmpty();
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

	private static class JavaScriptFormatter implements Formatter {

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

	private static class OptionalFormatter implements Formatter {

		private List<Formatter> formatters;

		public OptionalFormatter(List<Formatter> formatters) {
			this.formatters = formatters;
		}

		@Override
		public String format(long millis) {
			boolean optionalPresent = false;

			StringBuilder sb = new StringBuilder();
			for (Formatter f : formatters) {
				String formatted = f.format(millis);
				sb.append(formatted);
				if (!formatted.equals(f.format(0))) {
					optionalPresent = true;
				}
			}
			if (optionalPresent == true) {
				return sb.toString();
			} else {
				return "";
			}
		}

		public native String eval(String arg) /*-{
												return eval(arg);
												}-*/;

		public Integer getPrecision() {
			return minPrecision(formatters);
		};

	}

	private static abstract class TockenMatcher {
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
				int endIndex = previousMatch.getIndex() + previousMatch.getGroup(0).length() - 1;
				if (endIndex < format.length() - 1) {
					list.add(new StringFormatter(format.substring(endIndex + 1)));
				}
			}
			if (!list.isEmpty()) {
				return list;
			} else {
				return null;
			}
		}

		protected abstract Formatter buildFormatter(MatchResult match);

	}

	private abstract static class GroupPairMatcher extends TockenMatcher {
		private final Pattern startPattern;
		private final Pattern endPattern;

		public GroupPairMatcher(Pattern startPattern, Pattern endPattern) {
			super();
			this.startPattern = startPattern;
			this.endPattern = endPattern;
		}

		@Override
		public List<Formatter> compile(String format) {
			List<Formatter> list = new LinkedList<>();

			Matcher matcher;
			Matcher previousMatcher = null;
			while ((matcher = match(format, startPattern)) != null) {
				int startIndex = matcher.start();
				if (startIndex > 0) {
					list.add(new StringFormatter(format.substring(0, startIndex)));
					format = format.substring(startIndex);
				}

				SingleMatchResult match = findOuterMatchingTockensContent(format, startPattern, endPattern);
				if (match != null) {
					list.add(createFormatter(match));
					format = format.substring(match.end);
				} else {
					throw new IllegalArgumentException("formato non valido: '" + format + "'");
				}
				previousMatcher = matcher;
			}
			// tutto quello che rimane è una string
			if (previousMatcher != null && !format.isEmpty()) {
				list.add(new StringFormatter(format));
			}

			if (!list.isEmpty()) {
				return list;
			} else {
				return null;
			}
		}

		protected abstract Formatter createFormatter(SingleMatchResult match);
	}

	private static class JavaScriptMatcher extends GroupPairMatcher {
		private static final Pattern START_PATTERN = Pattern.compile("(" + Pattern.quote("%js{") + ")");
		private static final Pattern END_PATTERN = Pattern.compile("(" + Pattern.quote("}") + ")");
		private DurationFormatter durationFormatter;

		public JavaScriptMatcher(DurationFormatter durationFormatter) {
			super(START_PATTERN, END_PATTERN);
			this.durationFormatter = durationFormatter;
		}

		@Override
		protected Formatter createFormatter(SingleMatchResult match) {
			return new JavaScriptFormatter(durationFormatter.compile(match.group(1)));
		}
	}

	private static class OptionalMatcher extends GroupPairMatcher {
		private static final Pattern START_RX = Pattern.compile("(?:^|[^\\\\])(\\[)");
		private static final Pattern END_RX = Pattern.compile("[^\\\\](\\])");
		private final DurationFormatter durationFormatter;

		public OptionalMatcher(DurationFormatter durationFormatter) {
			super(START_RX, END_RX);
			this.durationFormatter = durationFormatter;
		}

		@Override
		protected Formatter createFormatter(SingleMatchResult match) {
			return new OptionalFormatter(durationFormatter.compile(match.group(1)));
		}

	}

	private List<TockenMatcher> builders = new LinkedList<>();
	private List<Formatter> formatters;

	/**
	 * Set the format. Available parameters:
	 * 
	 * <ul>
	 * <li>%sign minus sign if the time is negative or empty string (all other value
	 * will be without sign if present)</li>
	 * <li>%SIGN minus or plus sign (all other value will be without sign if
	 * present)</li>
	 * <li>%nosign no sign even if negative (all other value will be without sign if
	 * present)</li>
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

		builders.add(new JavaScriptMatcher(this));

		builders.add(new RegexMatcher(altRx("%nosign|%sign|%SIGN")) {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				if (in(match.getGroup(0), alt("%nosign"))) {
					return new StringFormatter("");
				} else {
					return new SignFormatter(in(match.getGroup(0), alt("%sign")));
				}
			}
		});

		builders.add(new OptionalMatcher(this));

		builders.add(new RegexMatcher(altRx("%tts|%TTS|%ts|%TS")) {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (in(match.getGroup(0), alt("%tts", "%TTS"))) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (in(match.getGroup(0), alt("%ts", "%tts"))) {
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
		builders.add(new RegexMatcher(altRx("%ss|%SS|%s|%S")) {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (in(match.getGroup(0), alt("%ss", "%SS"))) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (in(match.getGroup(0), alt("%s", "%ss"))) {
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
		builders.add(new RegexMatcher(altRx("%mm|%MM|%m|%M")) {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (in(match.getGroup(0), alt("%mm", "%MM"))) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (in(match.getGroup(0), alt("%m", "%mm"))) {
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
		builders.add(new RegexMatcher(altRx("%hh|%HH|%h|%H")) {
			@Override
			protected Formatter buildFormatter(MatchResult match) {
				int digits;
				if (in(match.getGroup(0), alt("%hh", "%HH"))) {
					digits = 2;
				} else {
					digits = 1;
				}
				if (in(match.getGroup(0), alt("%h", "%hh"))) {
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
		builders.add(new RegexMatcher(altRx("%d|%D")) {
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
		return Collections.singletonList((StringFormatter) new StringFormatter(format));
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
