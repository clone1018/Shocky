import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Utils;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;

public class ModuleDFWords extends Module {

	public static final Pattern tag = Pattern.compile("\\[(.+?)\\]");
	public static final Pattern split = Pattern.compile(":");
	public static final Pattern space = Pattern.compile("\\s");

	public Map<String, List<Word>> symbols;
	public Map<String, Word> words;
	public Map<String, Map<Word,String>> translations;

	protected Command cmd;

	@Override
	public String name() {
		return "dfwords";
	}

	@Override
	public void onEnable(File dir) {
		try {

			words = new HashMap<String, Word>();
			symbols = new HashMap<String, List<Word>>();
			translations = new HashMap<String, Map<Word,String>>();

			processFile(new File("modules/df/language_words.txt"));
			processFile(new File("modules/df/language_SYM.txt"));
			
			processFile(new File("modules/df/language_DWARF.txt"));
			processFile(new File("modules/df/language_ELF.txt"));
			processFile(new File("modules/df/language_GOBLIN.txt"));
			processFile(new File("modules/df/language_HUMAN.txt"));
			

			Command.addCommands(this, cmd = new CmdDFWords());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		Command.removeCommands(cmd);
		symbols = null;
		words = null;
		translations = null;
	}

	public void processFile(File file) throws IOException {
		if (!file.exists() || !file.isFile())
			return;

		String content;
		boolean valid = false;

		FileInputStream stream = new FileInputStream(file);
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			content = Charset.forName("cp437").decode(bb).toString();
		} finally {
			stream.close();
		}

		Matcher matcher = tag.matcher(content);

		String[] symbol = null;
		String translation = null;
		Word word = null;
		Noun noun = null;
		Adj adj = null;
		Verb verb = null;
		Prefix prefix = null;
		while (matcher.find()) {
			String[] parts = split.split(matcher.group(1));
			String type = parts[0];

			if (type.contentEquals("OBJECT")) {
				valid = parts[1].contentEquals("LANGUAGE");
				continue;
			}

			if (!valid)
				continue;

			if (type.contentEquals("WORD")) {
				word = new Word(parts[1]);
				words.put(parts[1], word);
				translation = null;
				symbol = null;
			} else if (type.contentEquals("SYMBOL")) {
				symbol = parts[1].split("_");
				translation = null;
				word = null;
			} else if (type.contentEquals("TRANSLATION")) {
				translation = parts[1];
				translations.put(translation, new HashMap<Word,String>());
				symbol = null;
				word = null;
			}

			if (word == null && symbol == null && translation == null)
				continue;

			if (noun != null) {
				if (type.contentEquals("THE_NOUN_SING")) {
					noun.flags |= Noun.THE_NOUN_SING;
					continue;
				} else if (type.contentEquals("THE_NOUN_PLUR")) {
					noun.flags |= Noun.THE_NOUN_PLUR;
					continue;
				} else if (type.contentEquals("THE_COMPOUND_NOUN_SING")) {
					noun.flags |= Noun.THE_COMPOUND_NOUN_SING;
					continue;
				} else if (type.contentEquals("THE_COMPOUND_NOUN_PLUR")) {
					noun.flags |= Noun.THE_COMPOUND_NOUN_PLUR;
					continue;
				} else if (type.contentEquals("OF_NOUN_SING")) {
					noun.flags |= Noun.OF_NOUN_SING;
					continue;
				} else if (type.contentEquals("OF_NOUN_PLUR")) {
					noun.flags |= Noun.OF_NOUN_PLUR;
					continue;
				} else if (type.contentEquals("FRONT_COMPOUND_NOUN_SING")) {
					noun.flags |= Noun.FRONT_COMPOUND_NOUN_SING;
					continue;
				} else if (type.contentEquals("FRONT_COMPOUND_NOUN_PLUR")) {
					noun.flags |= Noun.FRONT_COMPOUND_NOUN_PLUR;
					continue;
				} else if (type.contentEquals("REAR_COMPOUND_NOUN_SING")) {
					noun.flags |= Noun.REAR_COMPOUND_NOUN_SING;
					continue;
				} else if (type.contentEquals("REAR_COMPOUND_NOUN_PLUR")) {
					noun.flags |= Noun.REAR_COMPOUND_NOUN_PLUR;
					continue;
				} else {
					noun = null;
				}
			} else if (adj != null) {
				if (type.contentEquals("ADJ_DIST")) {
					adj.dist = Byte.parseByte(parts[1]);
					continue;
				} else if (type.contentEquals("THE_COMPOUND_ADJ")) {
					adj.flags |= Adj.THE_COMPOUND_ADJ;
					continue;
				} else if (type.contentEquals("FRONT_COMPOUND_ADJ")) {
					adj.flags |= Adj.FRONT_COMPOUND_ADJ;
					continue;
				} else if (type.contentEquals("REAR_COMPOUND_ADJ")) {
					adj.flags |= Adj.REAR_COMPOUND_ADJ;
					continue;
				} else {
					adj = null;
				}
			} else if (verb != null) {
				if (type.contentEquals("STANDARD_VERB")) {
					verb.flags |= Verb.STANDARD_VERB;
					continue;
				} else {
					verb = null;
				}
			} else if (prefix != null) {
				if (type.contentEquals("FRONT_COMPOUND_PREFIX")) {
					prefix.flags |= Prefix.FRONT_COMPOUND_PREFIX;
					continue;
				} else if (type.contentEquals("THE_COMPOUND_PREFIX")) {
					prefix.flags |= Prefix.THE_COMPOUND_PREFIX;
					continue;
				} else {
					prefix = null;
				}
			}

			if (type.contentEquals("NOUN")) {
				noun = new Noun(parts);
				if (word.nouns == null)
					word.nouns = new ArrayList<Noun>();
				word.nouns.add(noun);
				word.all.add(noun);
			} else if (type.contentEquals("ADJ")) {
				adj = new Adj(parts[1]);
				if (word.adjs == null)
					word.adjs = new ArrayList<Adj>();
				word.adjs.add(adj);
				word.all.add(adj);
			} else if (type.contentEquals("VERB")) {
				verb = new Verb(parts);
				if (word.verbs == null)
					word.verbs = new ArrayList<Verb>();
				word.verbs.add(verb);
				word.all.add(verb);
			} else if (type.contentEquals("PREFIX")) {
				prefix = new Prefix(parts[1]);
				if (word.prefixes == null)
					word.prefixes = new ArrayList<Prefix>();
				word.prefixes.add(prefix);
				word.all.add(prefix);
			} else if (symbol != null && type.contentEquals("S_WORD")) {
				for (int i = 0; i < symbol.length; i++) {
					if (!symbols.containsKey(symbol[i]))
						symbols.put(symbol[i], new ArrayList<Word>());
					symbols.get(symbol[i]).add(words.get(parts[1]));
				}
			} else if (translation != null && type.contentEquals("T_WORD")) {
				Word keyword = words.get(parts[1]);
				if (keyword != null)
					translations.get(translation).put(keyword, parts[2]);
			}
		}
	}

	public static <T> void put(Map<Short, List<T>> map, short key, T value) {
		if (!map.containsKey(key))
			map.put(key, new ArrayList<T>());

		map.get(key).add(value);
	}

	public static class Word {
		public Set<Part> all = new HashSet<Part>();
		public List<Noun> nouns;
		public List<Adj> adjs;
		public List<Verb> verbs;
		public List<Prefix> prefixes;

		public final String name;

		public Word(String name) {
			this.name = name;
		}

		public Set<Part> getParts(short flags) {
			HashSet<Part> set = new HashSet<Part>();
			for (Part p : all) {
				if ((p.getFlags() & flags) != 0)
					set.add(p);
			}
			if (set.isEmpty())
				return null;
			return set;
		}

		public Part getPart(Random rnd, short flags) {
			return Utils.rndCollection(getParts(flags), rnd);
		}

		public String getString(Random rnd, short flags, Map<Word,String> tranmap) {
			if (tranmap != null && tranmap.containsKey(this))
				return tranmap.get(this);
			Part part = getPart(rnd, flags);
			if (part == null)
				return null;
			return part.getString(flags);
		}
	}

	public Word get(Random rnd, short flags, String symbol) {
		if (symbol != null && !symbols.containsKey(symbol))
			return null;
		Collection<Word> iter = symbol != null ? symbols.get(symbol) : words.values();
		List<Word> list;
		if (flags == 0)
			list = new ArrayList<Word>(iter);
		else {
			list = new ArrayList<Word>();
			for (Word w : iter) {
				Set<Part> parts = w.getParts(flags);
				if (parts != null && !parts.isEmpty())
					list.add(w);
			}
		}
		if (list.isEmpty())
			return null;
		return list.get(rnd.nextInt(list.size()));
	}

	public String parse(CharSequence cs) {
		Random rnd = new Random();
		Map<Word,String> tranmap = null;
		String[] args = space.split(cs);
		int the = -1;
		int of = -1;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equalsIgnoreCase("the")) {
				the = i;
				continue;
			} else if (arg.equalsIgnoreCase("of")) {
				of = i;
				continue;
			}
			short flags = 0;
			if (the != -1 && the == i - 1)
				flags |= THE;
			if (of != -1 && of == i - 1)
				flags |= OF;
			Matcher m = tag.matcher(arg);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String[] parts = split.split(m.group(1));
				if (parts.length==2 && parts[0].contentEquals("TR")) {
					String trans = parts[1];
					if (translations.containsKey(trans))
						tranmap = translations.get(trans);
					m.appendReplacement(sb,"");
					continue;
				}
				char[] chars = parts[0].toCharArray();
				byte verbmode = 0;
				boolean matched = false;
				for (int o = 0; !matched && o < chars.length; o++) {
					char c = chars[o];
					switch (c) {
					case 's':
						flags |= SING;
						verbmode = 1;
						break;
					case 'p':
						flags |= PLUR;
						verbmode = 2;
						break;
					case 'r':
						verbmode = 3;
						break;
					case 't':
						verbmode = 4;
						break;
					case 'n':
						flags |= NOUN;
						String symbol = null;
						if (parts.length==2)
							symbol = parts[1];
						Word w = get(rnd, flags, symbol);
						m.appendReplacement(sb,w != null ? w.getString(rnd, flags,tranmap) : "");
						matched = true;
						break;
					case 'c':
						flags |= COMPOUND;
						short flags1 = (short) ((flags | FRONT) & ~PLUR);
						short flags2 = (short) (flags | REAR);
						
						String symbol1 = null;
						String symbol2 = null;
						if (parts.length==2) {
							symbol1 = symbol2 = parts[1];
						} else if (parts.length==3) {
							symbol1 = parts[1];
							symbol2 = parts[2];
						} 
						Word w1 = get(rnd, flags1, symbol1);
						Word w2 = get(rnd, flags2, symbol2);
						if (w1 != null && w2 != null)
							m.appendReplacement(sb,w1.getString(rnd, flags1,tranmap)+ '-'+ w2.getString(rnd,flags2,tranmap));
						else
							m.appendReplacement(sb,"");
						matched = true;
						break;
					case 'v':
						flags = VERB;
						symbol = null;
						if (parts.length==2)
							symbol = parts[1];
						w = get(rnd, flags, symbol);
						if (w != null) {
							if (tranmap != null && tranmap.containsKey(w)) {
								m.appendReplacement(sb, tranmap.get(w));
							} else {
								Verb v = ((Verb) w.getPart(rnd, flags));
								switch (verbmode) {
								case 0: m.appendReplacement(sb, v.base); break;
								case 1: m.appendReplacement(sb, v.presentSimple); break;
								case 2: m.appendReplacement(sb, v.pastTense); break;
								case 3: m.appendReplacement(sb, v.presentParticiple); break;
								case 4: m.appendReplacement(sb, v.pastParticiple); break;
								}
							}
						} else {
							m.appendReplacement(sb,"");
						}
						matched = true;
						break;
					case 'a':
						flags |= ADJ;
						w = null;
						symbol = null;
						if (parts.length==2)
							symbol = parts[1];
						w = get(rnd, flags, symbol);
						if (w != null)
							m.appendReplacement(sb,w.getString(rnd, flags,tranmap));
						else
							m.appendReplacement(sb,"");
						matched = true;
						break;
					}
				}
			}
			m.appendTail(sb);
			args[i] = sb.toString();
		}
		return StringTools.implode(args, " ");
	}

	public static final short NOUN = 0x0001;
	public static final short ADJ = 0x0002;
	public static final short VERB = 0x0004;
	public static final short PREFIX = 0x0008;
	public static final short SING = 0x0010;
	public static final short PLUR = 0x0020;
	public static final short THE = 0x0040;
	public static final short OF = 0x0080;
	public static final short COMPOUND = 0x0100;
	public static final short FRONT = 0x0200;
	public static final short REAR = 0x0400;

	public static interface Part {
		short getFlags();

		String getString(short flags);
	}

	public static class Noun implements Part {
		public static final short THE_NOUN_SING = THE | NOUN | SING;
		public static final short THE_NOUN_PLUR = THE | NOUN | PLUR;
		public static final short OF_NOUN_SING = OF | NOUN | SING;
		public static final short OF_NOUN_PLUR = OF | NOUN | PLUR;
		public static final short THE_COMPOUND_NOUN_SING = THE | COMPOUND | NOUN | SING;
		public static final short THE_COMPOUND_NOUN_PLUR = THE | COMPOUND | NOUN | PLUR;
		public static final short FRONT_COMPOUND_NOUN_SING = FRONT | COMPOUND | NOUN | SING;
		public static final short FRONT_COMPOUND_NOUN_PLUR = FRONT | COMPOUND | NOUN | PLUR;
		public static final short REAR_COMPOUND_NOUN_SING = REAR | COMPOUND | NOUN | SING;
		public static final short REAR_COMPOUND_NOUN_PLUR = REAR | COMPOUND | NOUN | PLUR;

		public final String singular;
		public final String plural;
		public short flags;

		public Noun(String[] words) {
			singular = words[1];
			plural = words.length == 3 ? words[2] : words[1];
		}

		@Override
		public short getFlags() {
			return flags;
		}

		@Override
		public String getString(short flags) {
			if ((flags & PLUR) != 0)
				return plural;
			return singular;
		}

	}

	public static class Adj implements Part {
		public static final short THE_COMPOUND_ADJ = THE | COMPOUND | ADJ;
		public static final short FRONT_COMPOUND_ADJ = FRONT | COMPOUND | ADJ;
		public static final short REAR_COMPOUND_ADJ = REAR | COMPOUND | ADJ;

		public final String adjective;
		public byte dist = 1;
		public short flags;

		public Adj(String word) {
			adjective = word;
		}

		@Override
		public short getFlags() {
			return flags;
		}

		@Override
		public String getString(short flags) {
			return adjective;
		}
	}

	public static class Verb implements Part {
		public static final short STANDARD_VERB = VERB;

		public final String base;
		public final String presentSimple;
		public final String pastTense;
		public final String pastParticiple;
		public final String presentParticiple;
		public short flags;

		public Verb(String[] words) {
			base = words[1];
			presentSimple = words[2];
			pastTense = words[3];
			pastParticiple = words[4];
			presentParticiple = words[5];
		}

		@Override
		public short getFlags() {
			return flags;
		}

		@Override
		public String getString(short flags) {
			return base;
		}
	}

	public static class Prefix implements Part {
		public static final short FRONT_COMPOUND_PREFIX = FRONT | COMPOUND | PREFIX;
		public static final short THE_COMPOUND_PREFIX = THE | COMPOUND | PREFIX;

		public final String word;
		public short flags;

		public Prefix(String word) {
			this.word = word;
		}

		@Override
		public short getFlags() {
			return flags;
		}

		@Override
		public String getString(short flags) {
			return word;
		}
	}

	public class CmdDFWords extends Command {
		public String command() {
			return "dfwords";
		}

		public String help(Parameters params) {
			StringBuilder sb = new StringBuilder();
			sb.append("dfwords/g");
			sb.append("\ndfwords {query} - returns a random line using given tags");
			return sb.toString();
		}

		@Override
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount == 0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}

			callback.append(parse(params.input));
		}
	}
}
