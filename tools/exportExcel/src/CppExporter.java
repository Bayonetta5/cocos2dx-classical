import java.io.File;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class CppExporter extends BaseExporter {
	@Override
	public void doExport(Workbook book, Sheet sheet, File file) throws CellFormatException, IOException {
		File outputDir = file.getParentFile();
		String className = "X" + firstCapital(sheet.getSheetName());
		
		// name and type of id column
		Row fieldRow = sheet.getRow(2);
		Row typeRow = sheet.getRow(3);
		String idName = fieldRow.getCell(0).getStringCellValue();
		String idType = typeRow.getCell(0).getStringCellValue();
		boolean idIsString = idType.equalsIgnoreCase("string");
		
		// json directory prefix
		String jsonDir = getOption("jsonDir");
		
		// header file
		StringBuilder hfile = new StringBuilder();
		hfile.append("// Auto generated by exportExcel, don't modify it\n")
			.append("#ifndef __" + className + "__\n")
			.append("#define __" + className + "__\n")
			.append("\n")
			.append("#include \"jsoncpp.h\"\n\n")
			.append("class " + className + " : public CCObject {\n")
			.append("protected:\n")
			.append("\t" + className + "();\n")
			.append("\n")
			.append("\tstatic void ensureLoaded();\n")
			.append("\n")
			.append("public:\n")
			.append("\tvirtual ~" + className + "();\n")
			.append("\tstatic " + className + "* create(" + (idIsString ? "const string&" : "int") + " key);\n")
			.append("\tstatic " + className + "* createWithIndex(int index);\n")
			.append("\tstatic " + className + "* createEmpty(" + (idIsString ? "const string&" : "int") + " _id);\n")
			.append("\tstatic int count();\n")
			.append("\tstatic Json::Value& query(const string& path);\n")
			.append("\tstatic int indexOf(" + (idIsString ? "const string&" : "int") + " _id);\n")
			.append("\tstatic int indexOf(" + className + "* t);\n")
			.append("\n")
			.append("\tvirtual bool initWithIndex(int index);\n")
			.append("\tvirtual bool initWithKey(" + (idIsString ? "const string&" : "int") + " key);\n")
			.append("\tvirtual bool initWithValue(const Json::Value& item);\n")
			.append("\tvirtual bool initWithId(" + (idIsString ? "const string&" : "int") + " _id);\n")
			.append("\tJson::Value toValue();\n")
			.append("\n");
		
		int len = sheet.getRow(2).getLastCellNum();
		for (int i = 0; i < len; i++) {
			if (sheet.getRow(2).getCell(i) == null || sheet.getRow(2).getCell(i).getStringCellValue().equals(""))
				continue;
			String field = sheet.getRow(2).getCell(i).getStringCellValue();
			Cell cell0 = sheet.getRow(0).getCell(i);
			if (cell0 != null && cell0.getCellComment() != null && !cell0.getCellComment().getString().equals("")) {
				hfile.append("\t/*" + sheet.getRow(0).getCell(i).getCellComment().getString() + "*/\n");
			}
			
			String type = sheet.getRow(3).getCell(i).getStringCellValue();
			if (type.equalsIgnoreCase("Byte") || type.equalsIgnoreCase("Int")) {
				hfile.append("\tCC_SYNTHESIZE(int, m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
			} else if (type.equalsIgnoreCase("bool")) {
				hfile.append("\tCC_SYNTHESIZE_BOOL(m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
			} else if (type.equalsIgnoreCase("Float")) {
				hfile.append("\tCC_SYNTHESIZE(float, m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
			} else if (type.equalsIgnoreCase("String") || type.equalsIgnoreCase("luafunc")) {
				hfile.append("\tCC_SYNTHESIZE_PASS_BY_REF(string, m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
			} else if(type.equalsIgnoreCase("StringArray")) {
				hfile.append("\tCC_SYNTHESIZE_PASS_BY_REF(CCArray, m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
				hfile.append("\tstring get" + firstCapital(field) + "At(int index);\n");
				hfile.append("\tint get" + firstCapital(field) + "Count();\n");
			} else if(type.equalsIgnoreCase("IntArray")) {
				hfile.append("\tCC_SYNTHESIZE_PASS_BY_REF(CCArray, m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
				hfile.append("\tint get" + firstCapital(field) + "At(int index);\n");
				hfile.append("\tint get" + firstCapital(field) + "Count();\n");
			} else if(type.equalsIgnoreCase("FloatArray")) {
				hfile.append("\tCC_SYNTHESIZE_PASS_BY_REF(CCArray, m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
				hfile.append("\tfloat get" + firstCapital(field) + "At(int index);\n");
				hfile.append("\tint get" + firstCapital(field) + "Count();\n");
			} else if(type.equalsIgnoreCase("BoolArray")) {
				hfile.append("\tCC_SYNTHESIZE_PASS_BY_REF(CCArray, m_" + firstLowercase(field) + ", " + firstCapital(field) + ");\n");
				hfile.append("\tbool get" + firstCapital(field) + "At(int index);\n");
				hfile.append("\tint get" + firstCapital(field) + "Count();\n");
			}
		}
		
		hfile.append("};\n");
		hfile.append("#endif // defined(__" + className +  "__)\n");
		try {
			File dstFile = new File(outputDir, className + ".h");
			writeFile(dstFile.getAbsolutePath(), hfile.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// cfile
		StringBuilder cfile = new StringBuilder()
			.append("#include \"" + className + ".h\"\n")
			.append("\n")
			.append("static Json::Value sJSON;\n")
			.append("static bool sLoaded = false;\n")
			.append("static int sCount = 0;\n")
			.append("\n")
			.append(className + "::" + className + "() {\n")
			.append("}\n")
			.append("\n")
			.append(className + "::~" + className + "() {\n")
			.append("}\n")
			.append("\n")
			.append("void " + className + "::ensureLoaded() {\n")
			.append("\tif(!sLoaded) {\n")
			.append("\t\tstring fullPath = CCUtils::getExternalOrFullPath(\"" + jsonDir + ("".equals(jsonDir) ? "" : "/") + className + ".json\");\n")
			.append("\t\tchar* raw = CCResourceLoader::loadCString(fullPath);\n")
			.append("\t\tJson::Reader reader;\n")
			.append("\t\treader.parse(raw, sJSON);\n")
			.append("\t\tfree(raw);\n")
			.append("\t\tsLoaded = true;\n")
			.append("\t\tsCount = sJSON.size();\n")
			.append("\t}\n")
			.append("}\n")
			.append("\n")
			.append(className + "* " + className + "::create(" + (idIsString ? "const string&" : "int") + " key) {\n")
			.append("\tensureLoaded();\n")
			.append("\t" + className + "* instance = new " + className + "();\n")
			.append("\tif(instance->initWithKey(key)) {\n")
			.append("\t\tCC_SAFE_AUTORELEASE_RETURN(instance, " + className + "*);\n")
			.append("\t}\n")
			.append("\tCC_SAFE_RELEASE(instance);\n")
			.append("\treturn nullptr;\n")
			.append("}\n")
			.append("\n")
			.append(className + "* " + className + "::createWithIndex(int index) {\n")
			.append("\tensureLoaded();\n")
			.append("\tif(index < 0 || index >= count()) {\n")
			.append("\t\treturn nullptr;\n")
			.append("\t} else {\n")
			.append("\t\t" + className + "* instance = new " + className + "();\n")
			.append("\t\tif(instance->initWithIndex(index)) {\n")
			.append("\t\t\tCC_SAFE_AUTORELEASE_RETURN(instance, " + className + "*);\n")
			.append("\t\t}\n")
			.append("\t\tCC_SAFE_RELEASE(instance);\n")
			.append("\t\treturn nullptr;\n")
			.append("\t}\n")
			.append("}\n")
			.append("\n")
			.append(className + "* " + className + "::createEmpty(" + (idIsString ? "const string&" : "int") + " _id) {\n")
			.append("\t" + className + "* instance = new " + className + "();\n")
			.append("\tif(instance->initWithId(_id)) {\n")
			.append("\t\tCC_SAFE_AUTORELEASE_RETURN(instance, " + className + "*);\n")
			.append("\t}\n")
			.append("\tCC_SAFE_RELEASE(instance);\n")
			.append("\treturn nullptr;\n")
			.append("}\n")
			.append("\n")
			.append("int " + className + "::count() {\n")
			.append("\tensureLoaded();\n")
			.append("\treturn sCount;\n")
			.append("}\n")
			.append("\n")
			.append("Json::Value& " + className + "::query(const string& path) {\n")
			.append("\tensureLoaded();\n")
			.append("\tJson::Path p(path);\n")
			.append("\treturn p.make(sJSON);\n")
			.append("}\n")
			.append("\n")
			.append("int " + className + "::indexOf(" + (idIsString ? "const string&" : "int") + " _id) {\n")
			.append("\tint size = count();\n")
			.append("\tint index = 0;\n")
			.append("\tJson::ValueIterator iter = sJSON.begin();\n")
			.append("\twhile(index < size) {\n")
			.append("\t\tconst Json::Value& item = *iter;\n")
			.append("\t\tif(item[\"" + firstCapital(idName) + "\"]." + (idIsString ? "asString" : "asInt") + "() == _id) {\n")
			.append("\t\t\treturn index;\n")
			.append("\t\t} else {\n")
			.append("\t\t\titer++;\n")
			.append("\t\t\tindex++;\n")
			.append("\t\t}\n")
			.append("\t}\n")
			.append("\treturn -1;\n")
			.append("}\n")
			.append("\n")
			.append("int " + className + "::indexOf(" + className + "* t) {\n")
			.append("\tint size = count();\n")
			.append("\tint index = 0;\n")
			.append("\tJson::ValueIterator iter = sJSON.begin();\n")
			.append("\twhile(index < size) {\n")
			.append("\t\tconst Json::Value& item = *iter;\n")
			.append("\t\tif(item[\"" + firstCapital(idName) + "\"]." + (idIsString ? "asString" : "asInt") + "() == t->get" + firstCapital(idName) + "()) {\n")
			.append("\t\t\treturn index;\n")
			.append("\t\t} else {\n")
			.append("\t\t\titer++;\n")
			.append("\t\t\tindex++;\n")
			.append("\t\t}\n")
			.append("\t}\n")
			.append("\treturn -1;\n")
			.append("}\n")
			.append("\n")
			.append("bool " + className + "::initWithIndex(int index) {\n")
			.append("\tJson::ValueIterator iter = sJSON.begin();\n")
			.append("\twhile(index-- > 0)\n")
			.append("\t\titer++;\n")
			.append("\treturn initWithValue(*iter);\n")
			.append("}\n")
			.append("\n")
			.append("bool " + className + "::initWithKey(" + (idIsString ? "const string&" : "int") + " key) {\n")
			.append(idIsString ? "\tconst char* _key = key.c_str();\n" : "\tchar _key[64];\n")
			.append(idIsString ? "" : "\tsprintf(_key, \"%d\", key);\n")
			.append("\tif(sJSON.isMember(_key)) {\n")
			.append("\t\treturn initWithValue(sJSON[_key]);\n")
			.append("\t} else {\n")
			.append("\t\treturn false;\n")
			.append("\t}\n")
			.append("}\n")
			.append("\n")
			.append("bool " + className + "::initWithValue(const Json::Value& item) {\n");

		// body of initWithValue
		for (int i = 0; i < len; i++) {
			if (sheet.getRow(3).getCell(i) == null || sheet.getRow(3).getCell(i).getStringCellValue().equals(""))
				continue;
			String field = sheet.getRow(2).getCell(i).getStringCellValue();
			String dataType = sheet.getRow(3).getCell(i).getStringCellValue();
			if (field == null || field.equals(""))
				continue;
			if (dataType.equalsIgnoreCase("Byte") || dataType.equalsIgnoreCase("int")) {
				cfile.append("\tm_" + firstLowercase(field) + " = item[\"" + firstCapital(field) + "\"].asInt();\n");
			} else if (dataType.equalsIgnoreCase("bool")) {
				cfile.append("\tm_" + firstLowercase(field) + " = item[\"" + firstCapital(field) + "\"].asBool();\n");
			} else if (dataType.equalsIgnoreCase("Float")) {
				cfile.append("\tm_" + firstLowercase(field) + " = item[\"" + firstCapital(field) + "\"].asDouble();\n");
			} else if (dataType.equalsIgnoreCase("String")) {
				cfile.append("\tm_" + firstLowercase(field) + " = item[\"" + firstCapital(field) + "\"].asString();\n");
			} else if(dataType.equalsIgnoreCase("luafunc")) {
				cfile.append("\tm_" + firstLowercase(field) + " = item[\"" + firstCapital(field) + "\"].asString();\n");
				cfile.append("\tm_" + firstLowercase(field) + " = CCUtils::replace(m_" + firstLowercase(field) + ", \"\\\\n\", \"\\n\");\n");
				cfile.append("\tm_" + firstLowercase(field) + " = CCUtils::replace(m_" + firstLowercase(field) + ", \"\\\\r\", \"\\r\");\n");
			} else if(dataType.equalsIgnoreCase("StringArray")) {
				cfile.append("\tCCArray& tmp_" + firstLowercase(field) + " = CCUtils::componentsOfString(item[\"" + firstCapital(field) + "\"].asString(), ',');\n");
				cfile.append("\tm_" + firstLowercase(field) + ".addObjectsFromArray(&tmp_" + firstLowercase(field) + ");\n");
			} else if(dataType.equalsIgnoreCase("IntArray")) {
				cfile.append("\tCCArray& tmp_" + firstLowercase(field) + " = CCUtils::intComponentsOfString(item[\"" + firstCapital(field) + "\"].asString(), ',');\n");
				cfile.append("\tm_" + firstLowercase(field) + ".addObjectsFromArray(&tmp_" + firstLowercase(field) + ");\n");
			} else if(dataType.equalsIgnoreCase("FloatArray")) {
				cfile.append("\tCCArray& tmp_" + firstLowercase(field) + " = CCUtils::floatComponentsOfString(item[\"" + firstCapital(field) + "\"].asString(), ',');\n");
				cfile.append("\tm_" + firstLowercase(field) + ".addObjectsFromArray(&tmp_" + firstLowercase(field) + ");\n");
			} else if(dataType.equalsIgnoreCase("BoolArray")) {
				cfile.append("\tCCArray& tmp_" + firstLowercase(field) + " = CCUtils::boolComponentsOfString(item[\"" + firstCapital(field) + "\"].asString(), ',');\n");
				cfile.append("\tm_" + firstLowercase(field) + ".addObjectsFromArray(&tmp_" + firstLowercase(field) + ");\n");
			}
		}
		
		// close initWithValue and start initWithId
		cfile.append("\treturn true;\n")
			.append("}\n")
			.append("\n")
			.append("bool " + className + "::initWithId(" + (idIsString ? "const string&" : "int") + " _id) {\n")
			.append("\tm_" + firstLowercase(idName) + " = _id;\n");
		
		// body of initWithId
		for (int i = 0; i < len; i++) {
			if (sheet.getRow(3).getCell(i) == null || sheet.getRow(3).getCell(i).getStringCellValue().equals(""))
				continue;
			String field = sheet.getRow(2).getCell(i).getStringCellValue();
			String dataType = sheet.getRow(3).getCell(i).getStringCellValue();
			if (field == null || field.equals("") || idName.equals(field))
				continue;
			if (dataType.equalsIgnoreCase("Byte") || dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("Float")) {
				cfile.append("\tm_" + firstLowercase(field) + " = 0;\n");
			} else if (dataType.equalsIgnoreCase("bool")) {
				cfile.append("\tm_" + firstLowercase(field) + " = false;\n");
			}
		}
		
		// close initWithId and start toValue
		cfile.append("\treturn true;\n")
			.append("}\n")
			.append("\n")
			.append("Json::Value " + className + "::toValue() {\n")
			.append("\tJson::Value v;\n");
			
		// body of toValue
		for (int i = 0; i < len; i++) {
			if (sheet.getRow(3).getCell(i) == null || sheet.getRow(3).getCell(i).getStringCellValue().equals(""))
				continue;
			String field = sheet.getRow(2).getCell(i).getStringCellValue();
			String dataType = sheet.getRow(3).getCell(i).getStringCellValue();
			if (field == null || field.equals(""))
				continue;
			if(dataType.equalsIgnoreCase("luafunc")) {
				cfile.append("\tstring tmp_" + firstLowercase(field) + " = CCUtils::replace(m_" + firstLowercase(field) + ", \"\\r\", \"\\\\r\");\n");
				cfile.append("\ttmp_" + firstLowercase(field) + " = CCUtils::replace(tmp_" + firstLowercase(field) + ", \"\\n\", \"\\\\n\");\n");
				cfile.append("\tv[\"" + firstCapital(field) + "\"] = tmp_" + firstLowercase(field) + ";\n");	
			} else if(dataType.equalsIgnoreCase("StringArray")) {
				cfile.append("\tv[\"" + firstCapital(field) + "\"] = CCUtils::joinString(m_" + firstLowercase(field) + ", ',');\n");	
			} else if(dataType.equalsIgnoreCase("IntArray")) {
				cfile.append("\tv[\"" + firstCapital(field) + "\"] = CCUtils::joinInt(m_" + firstLowercase(field) + ", ',');\n");	
			} else if(dataType.equalsIgnoreCase("FloatArray")) {
				cfile.append("\tv[\"" + firstCapital(field) + "\"] = CCUtils::joinFloat(m_" + firstLowercase(field) + ", ',');\n");	
			} else if(dataType.equalsIgnoreCase("BoolArray")) {
				cfile.append("\tv[\"" + firstCapital(field) + "\"] = CCUtils::joinBool(m_" + firstLowercase(field) + ", ',');\n");	
			} else {
				cfile.append("\tv[\"" + firstCapital(field) + "\"] = m_" + firstLowercase(field) + ";\n");	
			}
		}
		
		// close toValue
		cfile.append("\treturn v;\n")
			.append("}\n");
		
		// get method for array types
		for (int i = 0; i < len; i++) {
			if (sheet.getRow(3).getCell(i) == null || sheet.getRow(3).getCell(i).getStringCellValue().equals(""))
				continue;
			String field = sheet.getRow(2).getCell(i).getStringCellValue();
			String dataType = sheet.getRow(3).getCell(i).getStringCellValue();
			if (field == null || field.equals(""))
				continue;
			if(dataType.equalsIgnoreCase("StringArray")) {
				cfile.append("\nstring " + className + "::get" + firstCapital(field) + "At(int index) {\n")
					.append("\tif(index < 0 || index >= m_" + firstLowercase(field) + ".count())\n")
					.append("\t\treturn \"\";\n")
					.append("\tCCString* cc = (CCString*)m_" + firstLowercase(field) + ".objectAtIndex(index);\n")
					.append("\treturn cc->getCString();\n")
					.append("}\n")
					.append("\nint " + className + "::get" + firstCapital(field) + "Count() {\n")
					.append("\treturn get" + firstCapital(field) + "().count();\n")
					.append("}\n");
			} else if(dataType.equalsIgnoreCase("IntArray")) {
				cfile.append("\nint " + className + "::get" + firstCapital(field) + "At(int index) {\n")
					.append("\tif(index < 0 || index >= m_" + firstLowercase(field) + ".count())\n")
					.append("\t\treturn 0;\n")
					.append("\tCCInteger* cc = (CCInteger*)m_" + firstLowercase(field) + ".objectAtIndex(index);\n")
					.append("\treturn cc->getValue();\n")
					.append("}\n")
					.append("\nint " + className + "::get" + firstCapital(field) + "Count() {\n")
					.append("\treturn get" + firstCapital(field) + "().count();\n")
					.append("}\n");
			} else if(dataType.equalsIgnoreCase("FloatArray")) {
				cfile.append("\nfloat " + className + "::get" + firstCapital(field) + "At(int index) {\n")
					.append("\tif(index < 0 || index >= m_" + firstLowercase(field) + ".count())\n")
					.append("\t\treturn 0;\n")
					.append("\tCCFloat* cc = (CCFloat*)m_" + firstLowercase(field) + ".objectAtIndex(index);\n")
					.append("\treturn cc->getValue();\n")
					.append("}\n")
					.append("\nint " + className + "::get" + firstCapital(field) + "Count() {\n")
					.append("\treturn get" + firstCapital(field) + "().count();\n")
					.append("}\n");
			} else if(dataType.equalsIgnoreCase("BoolArray")) {
				cfile.append("\nbool " + className + "::get" + firstCapital(field) + "At(int index) {\n")
					.append("\tif(index < 0 || index >= m_" + firstLowercase(field) + ".count())\n")
					.append("\t\treturn false;\n")
					.append("\tCCBool* cc = (CCBool*)m_" + firstLowercase(field) + ".objectAtIndex(index);\n")
					.append("\treturn cc->getValue();\n")
					.append("}\n")
					.append("\nint " + className + "::get" + firstCapital(field) + "Count() {\n")
					.append("\treturn get" + firstCapital(field) + "().count();\n")
					.append("}\n");
			}
		}

		try {
			File dstFile = new File(outputDir, className + ".cpp");
			writeFile(dstFile.getAbsolutePath(), cfile.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
