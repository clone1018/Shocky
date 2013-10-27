import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;

//import opennlp.tools.namefind.NameFinderME;
//import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.DetokenizationDictionary;
import opennlp.tools.tokenize.DetokenizationDictionary.Operation;
import opennlp.tools.tokenize.Detokenizer;
import opennlp.tools.tokenize.DictionaryDetokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import pl.shockah.StringTools;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.interfaces.ILua;

public class ModuleNLP extends Module implements ILua {
	
	private static final DetokenizationDictionary detokenizerDict;
	private TokenizerModel tokenizerModel;
	private POSModel posModel;
	private SentenceModel sentenceModel;
	//private TokenNameFinderModel nameModel;
	
	private Command cmdPOS;
	private Command cmdPOSReplace;
	
	static {
		String tokens[] = new String[]{".", "!", "?", ",", "$", "(", ")", "[", "]", "<", ">", "\"", "'", ":"};
		Operation operations[] = new Operation[]{
		        Operation.MOVE_LEFT,
		        Operation.MOVE_LEFT,
		        Operation.MOVE_LEFT,
		        Operation.MOVE_LEFT,
		        Operation.MOVE_RIGHT,
		        Operation.MOVE_RIGHT,
		        Operation.MOVE_LEFT,
		        Operation.MOVE_RIGHT,
		        Operation.MOVE_LEFT,
		        Operation.MOVE_RIGHT,
		        Operation.MOVE_LEFT,
		        Operation.RIGHT_LEFT_MATCHING,
		        Operation.RIGHT_LEFT_MATCHING,
		        Operation.MOVE_BOTH
		      };
		detokenizerDict = new DetokenizationDictionary(tokens, operations);
	}

	@Override
	public String name() {
		return "nlp";
	}

	@Override
	public void onEnable(File dir) {
		InputStream is = null;
		try {
			is = new FileInputStream(new File("data", "en-token.bin"));
			tokenizerModel = new TokenizerModel(is);
			is.close();
			is = new FileInputStream(new File("data", "en-pos-maxent.bin"));
			posModel = new POSModel(is);
			is.close();
			is = new FileInputStream(new File("data", "en-sent.bin"));
			sentenceModel = new SentenceModel(is);
			/*is.close();
			is = new FileInputStream(new File("data", "en-ner-person.bin"));
			nameModel = new TokenNameFinderModel(is);*/
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null)
				try {is.close();} catch (IOException e) {}
		}
		Command.addCommands(this, cmdPOS = new CmdPOS(), cmdPOSReplace = new CmdPOSReplace());
	}

	@Override
	public void onDisable() {
		Command.removeCommands(cmdPOS,cmdPOSReplace);
		tokenizerModel = null;
		posModel = null;
		sentenceModel = null;
		//nameModel = null;
	}
	
	public Span[] tokenize(String str) {
		Tokenizer tokenizer = new TokenizerME(tokenizerModel);
		return tokenizer.tokenizePos(str);
	}
	
	public class TokenizeFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			String s = arg.checkjstring();
			return convert(Span.spansToStrings(tokenize(s), s));
		}
	}
	
	public String detokenize(String[] tokens) {
		Detokenizer detokenizer = new DictionaryDetokenizer(detokenizerDict);
		return detokenizer.detokenize(tokens, "");
	}
	
	public class DetokenizeFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			return valueOf(detokenize(convert(arg)));
		}
	}
	
	public String[] getPOSTags(String[] tokens) {
		POSTagger tagger = new POSTaggerME(posModel);
		return tagger.tag(tokens);
	}
	
	public class POSFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			return convert(getPOSTags(convert(arg)));
		}
	}
	
	public String[] getSentences(String str) {
		SentenceDetectorME detect = new SentenceDetectorME(sentenceModel);
		return detect.sentDetect(str);
	}
	
	public class SentenceFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			return convert(getSentences(arg.checkjstring()));
		}
	}
	
	private static String[] convert(LuaValue v) {
		LuaTable t = v.checktable();
		LinkedList<String> list = new LinkedList<String>();
		for ( int i=0; !(v = t.rawget(++i)).isnil(); )
			list.add(v.checkjstring());
		return list.toArray(new String[0]);
	}
	
	private static LuaValue convert(String[] s) {
		LuaValue[] v = new LuaValue[s.length];
		for (int i = 0; i < s.length; ++i)
			v[i] = LuaValue.valueOf(s[i]);
		return LuaValue.tableOf(null,v);
	}
	
	@Override
	public void setupLua(LuaTable env) {
		LuaTable t = new LuaTable();
		t.rawset("tok", new TokenizeFunction());
		t.rawset("detok", new DetokenizeFunction());
		t.rawset("pos", new POSFunction());
		t.rawset("rpos", new POSReplaceFunction());
		t.rawset("sent", new SentenceFunction());
		env.set("nlp", t);
	}
	
	/*public class NameReplaceFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			return convert(getSentences(arg.checkjstring()));
		}
	}
	
	public String[] nameReplace(String sentence) {
		NameFinderME nameFinder = new NameFinderME(nameModel);
		String[] tokens = tokenize(sentence);
		Span[] spans = nameFinder.find(tokens);
		for (int i = 0; i < spans.length; ++i) {
			String name = StringTools.implode(tokens,spans[i].getStart()-1,spans[i].getEnd()-1," ");
			System.out.println(name);
		}
		return null;
	}*/
	
	public class POSReplaceFunction extends ThreeArgFunction {

		@Override
		public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
			String sentence = arg1.checkjstring();
			float chance = arg2.checknumber().tofloat();
			LuaTable table = arg3.checktable();
			String[][] replacements = new String[table.getn().toint()][];
			LuaValue v;
			for ( int i=0; !(v = table.rawget(++i)).isnil(); ) {
				LuaTable t = v.checktable();
				replacements[i-1]=new String[]{t.get("type").checkjstring(),t.get("word").checkjstring()};
			}
			
			return valueOf(posReplace(sentence, chance, replacements));
		}
	}
	
	public String posReplace(String sentence, float chance, String[][] replacements) {
		Map<String,LinkedList<String>> wordMap = new HashMap<String,LinkedList<String>>();
		try {
			for (int i = replacements.length-1; i >= 0; --i) {
				String pos = replacements[i][0];
				String word = replacements[i][1];
				LinkedList<String> list = wordMap.get(pos);
				if (list == null) {
					list = new LinkedList<String>();
					wordMap.put(pos, list);
				}
				list.add(word);
			}
		} catch(Exception e) {
			return null;
		}
		
		Span[] spans = tokenize(sentence);
		String[] tokens = Span.spansToStrings(spans, sentence);
		String[] tags = getPOSTags(tokens);
		
		boolean shouldRun = false;
		for (int i = tags.length-1; !shouldRun && i >= 0; --i)
			if (wordMap.containsKey(tags[i]))
				shouldRun = true;
		if (!shouldRun)
			return sentence;
		
		Random rnd = new Random();
		StringBuilder sb = null;
		
		int replaces = 0;
		int runs = 0;
		while (replaces == 0 && runs < 100) {
			int start = 0;
			sb = new StringBuilder(sentence.length());
			for (int i = 0; i < tokens.length && i < tags.length; ++i) {
				String tag = tags[i];
				String token = tokens[i];
				if (token.contentEquals("'") || token.contentEquals(">"))
					continue;
				if (wordMap.containsKey(tag) && rnd.nextFloat() < chance) {
					LinkedList<String> wordList = wordMap.get(tag);
					String word=
						(wordList.size()==1) ?
							wordList.element() : 
							wordList.get(rnd.nextInt(wordList.size()));
					Span span = spans[i];
					if (token.charAt(0)=='<' && token.charAt(token.length()-1)=='>')
						span = new Span(span.getStart()+1,span.getEnd()-1);
					else if (token.charAt(0)=='\'' && token.charAt(token.length()-1)!='\'' && (i+1 < tokens.length) && tokens[i+1].contentEquals("'"))
						span = new Span(span.getStart()+1,span.getEnd());
					else if (token.charAt(0)=='<' && token.charAt(token.length()-1)!='>' && (i+1 < tokens.length) && tokens[i+1].contentEquals(">"))
						span = new Span(span.getStart()+1,span.getEnd());
					sb.append(sentence.substring(start, span.getStart()));
					boolean upper = true;
					for (int o = 0; upper && o < token.length(); ++o)
						if (Character.isLowerCase(token.charAt(o)))
							upper = false;
					sb.append(upper ? word.toUpperCase() : word);
					start=span.getEnd();
					++replaces;
				}
			}
			sb.append(sentence.substring(start));
			++runs;
		}
		return sb.toString();
	}
	
	public class CmdPOS extends Command {
		public String command() {return "pos";}
		public String help(Parameters params) {
			return "pos {sentence} - adds the parts of speech after each word/character.";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount==0) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			String[] tokens = Span.spansToStrings(tokenize(params.input), params.input);
			String[] tags = getPOSTags(tokens);
			
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < tokens.length && i < tags.length; ++i) {
				if (i > 0)
					sb.append(' ');
				sb.append(tokens[i]).append('/').append(tags[i]);
			}
			callback.append(StringTools.limitLength(sb));
		}
	}
	
	public class CmdPOSReplace extends Command {
		public String command() {return "rpos";}
		public String help(Parameters params) {
			return "rpos {chance} {total} {{pos} {replacement}} {sentence} - replaces each part of speech with word provided.";
		}

		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount<3) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String[][] replacements;
			float chance;
			String sentence;
			try {
				chance = Float.parseFloat(params.nextParam());
				int total = Integer.parseInt(params.nextParam());
				if (params.tokenCount<3+(total<<1)) {
					callback.type = EType.Notice;
					callback.append(help(params));
					return;
				}
				replacements = new String[total][];
				for (int i = 0; i < total; ++i) {
					String pos = params.nextParam();
					String word = params.nextParam();
					replacements[i] = new String[] {pos, word};
				}
				sentence = params.getParams(0);
			} catch(Exception e) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String s = posReplace(sentence, chance, replacements);
			if (s == null) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			callback.append(StringTools.limitLength(s));
		}
	}
}
