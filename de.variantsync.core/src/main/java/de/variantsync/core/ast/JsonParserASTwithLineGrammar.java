package de.variantsync.core.ast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * Uses gson to import and export ASTs to and from json. A generic use of
 * grammar is not possible, hence for each grammar a different class has to be
 * created. In this case the used grammar is LineGrammar.
 * 
 * @author Jeremia Heinle
 *
 */
public class JsonParserASTwithLineGrammar {

	// json parser uses a custom policy to define the name of the field
	// inside of the json
	static FieldNamingStrategy customPolicy = new FieldNamingStrategy() {
		@Override
		public String translateName(Field f) {

			switch (f.getName()) {
			case "id":
				return "uuid";
			case "type":
				return "grammar_type";
			default:
				return f.getName();
			}
		}

	};

	static Gson prettyStringGsonBuilder = new GsonBuilder().setPrettyPrinting().setFieldNamingStrategy(customPolicy)
			.create();

	public static String toJson(AST<LineGrammar, String> ast) {

		Type type = new TypeToken<AST<LineGrammar, String>>() {
		}.getType();

		return prettyStringGsonBuilder.toJson(ast, type);
	}

	public static AST<LineGrammar, String> toAST(String json) {

		Type type = new TypeToken<AST<LineGrammar, String>>() {
		}.getType();

		return prettyStringGsonBuilder.fromJson(json, type);
	}

	public static String exportAST(Path path, AST<LineGrammar, String> ast) {

		String content = toJson(ast);
		try {
			Files.writeString(path, content);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return content;
	}

	public static AST<LineGrammar, String> importAST(Path path) {

		String json = "";
		try {
			json = Files.readString(path);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return toAST(json);
	}

}
