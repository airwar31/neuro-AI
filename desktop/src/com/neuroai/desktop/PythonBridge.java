package com.neuroai.desktop;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Supervised Process Bridge between Java Desktop GUI and Python AI Engine.
 * Executes python CLI backend with robust JSON data exchange via temporary files.
 */
public class PythonBridge {
    private final File repoRoot;
    private final String pythonExecutable;

    public PythonBridge() {
        this.repoRoot = findRepoRoot();
        this.pythonExecutable = detectPython();
    }

    private File findRepoRoot() {
        File current = new File(System.getProperty("user.dir"));
        if (new File(current, "src").exists() && new File(current, "models").exists()) {
            return current;
        }
        File parent = current.getParentFile();
        if (parent != null && new File(parent, "src").exists()) {
            return parent;
        }
        return current;
    }

    private String detectPython() {
        // 1. Check virtual environment
        File venvPyWin = new File(repoRoot, ".venv/Scripts/python.exe");
        if (venvPyWin.exists()) return venvPyWin.getAbsolutePath();
        File venvPyUnix = new File(repoRoot, ".venv/bin/python3");
        if (venvPyUnix.exists()) return venvPyUnix.getAbsolutePath();

        // 2. Default system command
        return System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3";
    }

    /**
     * Inspects an MRI file or directory (checks files, contrast tags, lesion volume).
     */
    public Map<String, Object> inspectMRI(String path, String contrastMode) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add("src/backend_cli.py");
        command.add("inspect-mri");
        command.add("--path");
        command.add(path);
        command.add("--contrast");
        command.add(contrastMode);

        String jsonOutput = runProcess(command);
        return parseSimpleJson(jsonOutput);
    }

    public Map<String, Object> renderMRI(String path, int sliceIndex, String axis) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add("src/backend_cli.py");
        command.add("render-mri-slice");
        command.add("--path");
        command.add(path);
        command.add("--slice-idx");
        command.add(String.valueOf(sliceIndex));
        command.add("--axis");
        command.add(axis);

        String jsonOutput = runProcess(command);
        return parseSimpleJson(jsonOutput);
    }

    /**
     * Runs clinical diagnosis using the trained ensemble model.
     */
    public Map<String, Object> predict(Map<String, Object> patientParams) throws Exception {
        // Write params to a temp JSON file to guarantee 100% shell escaping safety
        File tempFile = File.createTempFile("neuroai_patient_", ".json");
        tempFile.deleteOnExit();

        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : patientParams.entrySet()) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue());
            i++;
        }
        sb.append("}");

        Files.writeString(tempFile.toPath(), sb.toString(), StandardCharsets.UTF_8);

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add("src/backend_cli.py");
        command.add("predict");
        command.add("--file");
        command.add(tempFile.getAbsolutePath());

        String jsonOutput = runProcess(command);
        return parseSimpleJson(jsonOutput);
    }

    private String runProcess(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(repoRoot);
        pb.redirectErrorStream(true);
        pb.environment().put("PYTHONIOENCODING", "UTF-8");
        pb.environment().put("PYTHONUTF8", "1");

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python execution failed (code " + exitCode + "):\n" + output);
        }

        return output.toString().trim();
    }

    /**
     * Robust, zero-dependency recursive JSON parser for backend responses.
     * Supports nested Maps, Lists, Numbers, Booleans, Strings, and null.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseSimpleJson(String json) {
        Object parsed = parseJsonValue(json);
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        return new LinkedHashMap<>();
    }

    public static Object parseJsonValue(String raw) {
        if (raw == null) return null;
        String val = raw.trim();
        if (val.isEmpty()) return null;

        if (val.startsWith("{") && val.endsWith("}")) {
            Map<String, Object> map = new LinkedHashMap<>();
            String inner = val.substring(1, val.length() - 1).trim();
            for (String token : splitTokens(inner)) {
                int colonIdx = findColonIndex(token);
                if (colonIdx != -1) {
                    String k = token.substring(0, colonIdx).trim();
                    if (k.startsWith("\"") && k.endsWith("\"") && k.length() >= 2) {
                        k = k.substring(1, k.length() - 1);
                    }
                    String v = token.substring(colonIdx + 1).trim();
                    map.put(k, parseJsonValue(v));
                }
            }
            return map;
        } else if (val.startsWith("[") && val.endsWith("]")) {
            List<Object> list = new ArrayList<>();
            String inner = val.substring(1, val.length() - 1).trim();
            for (String token : splitTokens(inner)) {
                list.add(parseJsonValue(token));
            }
            return list;
        } else if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            return unescapeString(val.substring(1, val.length() - 1));
        } else if ("true".equalsIgnoreCase(val)) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(val)) {
            return Boolean.FALSE;
        } else if ("null".equalsIgnoreCase(val)) {
            return null;
        } else {
            try {
                if (val.contains(".") || val.contains("e") || val.contains("E")) {
                    return Double.parseDouble(val);
                } else {
                    return Long.parseLong(val);
                }
            } catch (NumberFormatException e) {
                return val;
            }
        }
    }

    private static int findColonIndex(String token) {
        boolean inQuotes = false;
        boolean escape = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inQuotes = !inQuotes; continue; }
            if (c == ':' && !inQuotes) return i;
        }
        return -1;
    }

    private static List<String> splitTokens(String content) {
        List<String> tokens = new ArrayList<>();
        if (content == null || content.isEmpty()) return tokens;

        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                current.append(c);
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }
            if (!inQuotes) {
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
                else if (c == '[') bracketDepth++;
                else if (c == ']') bracketDepth--;
                else if (c == ',' && braceDepth == 0 && bracketDepth == 0) {
                    if (current.length() > 0) {
                        tokens.add(current.toString().trim());
                        current.setLength(0);
                    }
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0 && !current.toString().trim().isEmpty()) {
            tokens.add(current.toString().trim());
        }
        return tokens;
    }

    private static String unescapeString(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '\\': sb.append('\\'); i++; break;
                    case '"':  sb.append('"');  i++; break;
                    case '/':  sb.append('/');  i++; break;
                    case 'b':  sb.append('\b'); i++; break;
                    case 'f':  sb.append('\f'); i++; break;
                    case 'n':  sb.append('\n'); i++; break;
                    case 'r':  sb.append('\r'); i++; break;
                    case 't':  sb.append('\t'); i++; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            try {
                                int code = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                                sb.append((char) code);
                                i += 5;
                                break;
                            } catch (NumberFormatException ignored) {}
                        }
                        sb.append(c);
                        break;
                    default:
                        sb.append(next);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println("Testing PythonBridge connection...");
        try {
            PythonBridge pb = new PythonBridge();
            Map<String, Object> testParams = new LinkedHashMap<>();
            testParams.put("age", 36);
            testParams.put("gender", 0);
            testParams.put("mmse", 28);
            testParams.put("edss_score", 3.5);
            testParams.put("t2_lesions_count", 19);
            testParams.put("oligoclonal_bands", 1);

            Map<String, Object> res = pb.predict(testParams);
            System.out.println("Bridge test result: " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
