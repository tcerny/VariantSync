package de.variantsync.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import de.variantsync.core.ast.AST;
import de.variantsync.core.ast.JsonParserASTwithLineGrammar;
import de.variantsync.core.ast.LineGrammar;
import org.junit.Before;
import org.junit.Test;

//TODO. Add compare method to AST and then compare ASTs with this methods.


public class JsonParserTestASTwithLineGrammar {

	AST<LineGrammar, String> exampleAst;
	Path exmaplePath;

	@Before
	public void InitJsonTest() throws IOException {
		// init
		exampleAst = new AST<>(LineGrammar.Directory, "src");
		AST<LineGrammar, String> mainJava = new AST<>(LineGrammar.TextFile, "Main.java");
		exampleAst.addChild(mainJava);
		mainJava.addChildren(Arrays.asList(new AST<>(LineGrammar.Line, "public class Main {"),
				new AST<>(LineGrammar.Line, "    public static void main(String[] args)"),
				new AST<>(LineGrammar.Line, "        System.out.println(\"Hello World\");"),
				new AST<>(LineGrammar.Line, "    }"), new AST<>(LineGrammar.Line, "}")));

		exmaplePath = Path.of("out.txt");

	}

	@Test
	public void TestJsonParserAST() {

		// export to json
		String json = JsonParserASTwithLineGrammar.toJson(exampleAst);

		// import ast from json
		AST<LineGrammar, String> importAST = JsonParserASTwithLineGrammar.toAST(json);

		// toJson imported AST
		String importedJson = JsonParserASTwithLineGrammar.toJson(importAST);

		// print
		System.out.println("FileFirst:" + json);

		System.out.println("FileSecond:" + importedJson);

		// compare json
		assertEquals(json, importedJson);

	}

	@Test
	public void TestJsonParserASTtoFile() throws IOException {

		// export to json file
		String json = JsonParserASTwithLineGrammar.exportAST(exmaplePath, exampleAst);

		// import ast from file
		AST<LineGrammar, String> importedAST = JsonParserASTwithLineGrammar.importAST(exmaplePath);

		// toJson imported AST
		String importedJson = JsonParserASTwithLineGrammar.toJson(importedAST);

		// print
		System.out.println("FileFirst:" + json);

		System.out.println("FileSecond:" + importedJson);

		// compare json
		assertEquals(json, importedJson);

		// delete created file
		Files.delete(exmaplePath);
	}

	@Test
	public void TestJsonParserASTtoFileToString() throws IOException {

		// export to json file
		JsonParserASTwithLineGrammar.exportAST(exmaplePath, exampleAst);

		// import ast from file
		AST<LineGrammar, String> importedAST = JsonParserASTwithLineGrammar.importAST(exmaplePath);

		// get toString
		String astString = exampleAst.toString();
		String importedString = importedAST.toString();

		// print
		System.out.println("FileFirst:" + astString);

		System.out.println("FileSecond:" + importedString);

		// compare json
		assertEquals(astString, importedString);

		// delete created file
		Files.delete(exmaplePath);
	}

}
