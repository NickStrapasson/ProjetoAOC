import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contém a lógica central do simulador MIPS.
 * Esta classe é responsável por analisar o código assembly (parsing), gerenciar o
 * estado dos registradores e da memória simulada, e executar cada instrução MIPS,
 * tratando operações aritméticas, acesso à memória e chamadas de sistema (syscalls).
 */
class SimuladorMIPS32 {
    private Map<String, Integer> registradores;
    private int cont;
    private List<String> instrucoes;
    private List<String> saidas;
    private List<String> bin;
    private Map<String, Integer> rotulos;
    private Map<String, Object> dados;
    private Map<String, ArrayList<Integer>> vetores;

    public SimuladorMIPS32() {
        this.registradores = inicializarRegistradores();
        this.cont = 0;
        this.instrucoes = new ArrayList<>();
        this.saidas = new ArrayList<>();
        this.bin = new ArrayList<>();
        this.rotulos = new HashMap<>();
        this.dados = new HashMap<>();
        this.vetores = new HashMap<>();
        resetar();
    }

    private Map<String, Integer> inicializarRegistradores() {
        Map<String, Integer> regs = new LinkedHashMap<>();
        String[] nomesRegs = {
            "$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
            "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
        };
        for (String nomeReg : nomesRegs) {
            regs.put(nomeReg, 0);
        }
        return regs;
    }

    public void carregarInstrucoes(String nomeArquivo) throws IOException {
        resetar();
        try (BufferedReader leitor = new BufferedReader(new FileReader(nomeArquivo))) {
            parseCodigo(leitor);
        }
    }

    public void carregarInstrucoesDeString(String codigoCompleto) throws IOException {
        resetar();
        try (BufferedReader leitor = new BufferedReader(new StringReader(codigoCompleto))) {
            parseCodigo(leitor);
        }
    }

    private void parseCodigo(BufferedReader leitor) throws IOException {
        String linha;
        String secaoAtual = null;
        int enderecoInstrucaoAtual = 0;
        int linhaNum = 0;

        while ((linha = leitor.readLine()) != null) {
            linhaNum++;
            linha = linha.split("#")[0].trim();
            if (linha.isEmpty()) {
                continue;
            }

            if (linha.equalsIgnoreCase(".data")) {
                secaoAtual = "data";
                continue;
            } else if (linha.equalsIgnoreCase(".text")) {
                secaoAtual = "text";
                continue;
            }

            if (secaoAtual == null) {
                if (!linha.startsWith(".")) {
                    secaoAtual = "text";
                } else {
                    continue;
                }
            }

            if ("data".equals(secaoAtual)) {
                String[] partes = linha.split("\\s+", 3);
                if (partes.length >= 2) {
                    String varLabel = partes[0];
                    if (varLabel.endsWith(":")) {
                        varLabel = varLabel.substring(0, varLabel.length() - 1);
                    }
                    String tipo = partes[1];
                    String valorStr = (partes.length > 2) ? partes[2] : "";

                    try {
                        if (".word".equals(tipo) || ".byte".equals(tipo)) {
                            String[] valoresArrayStr = valorStr.split(",");
                            ArrayList<Integer> valoresInt = new ArrayList<>();
                            for (String v : valoresArrayStr) {
                                valoresInt.add(Integer.decode(v.trim()));
                            }
                            vetores.put(varLabel, valoresInt);
                        } else if (".space".equals(tipo)) {
                            int tamanhoEmBytes = Integer.decode(valorStr.trim());
                            int numWords = (tamanhoEmBytes + 3) / 4;
                            ArrayList<Integer> spaceVec = new ArrayList<>(numWords);
                            for(int i = 0; i < numWords; i++) spaceVec.add(0);
                            vetores.put(varLabel, spaceVec);
                        } else if (".asciiz".equals(tipo)) {
                            String stringValor = valorStr.trim();
                            if (stringValor.startsWith("\"") && stringValor.endsWith("\"")) {
                                stringValor = stringValor.substring(1, stringValor.length() - 1)
                                                         .replace("\\n", "\n")
                                                         .replace("\\t", "\t")
                                                         .replace("\\\"", "\"")
                                                         .replace("\\\\", "\\");
                            } else {
                                 System.err.println("Linha " + linhaNum + ": Aviso: String .asciiz sem aspas: " + linha);
                            }
                            dados.put(varLabel, stringValor);
                        } else {
                            dados.put(varLabel, Integer.decode(valorStr.trim()));
                        }
                    } catch (NumberFormatException e) {
                        throw new IOException("Linha " + linhaNum + ": Valor numérico inválido para '" + varLabel + "': " + valorStr, e);
                    }
                } else {
                     throw new IOException("Linha " + linhaNum + ": Linha de dados mal formatada: '" + linha + "'");
                }
            } else if ("text".equals(secaoAtual)) {
                if (linha.endsWith(":")) {
                    String rotuloNome = linha.substring(0, linha.length() - 1).trim();
                    if (!rotuloNome.isEmpty()) {
                        if (rotulos.containsKey(rotuloNome)) throw new IOException("Linha " + linhaNum + ": Rótulo duplicado '" + rotuloNome + "'");
                        rotulos.put(rotuloNome, enderecoInstrucaoAtual);
                    } else {
                        throw new IOException("Linha " + linhaNum + ": Rótulo vazio: '" + linha + "'");
                    }
                } else {
                    instrucoes.add(linha);
                    enderecoInstrucaoAtual++;
                }
            }
        }
    }


    public void executar() {
        saidas.clear();
        bin.clear();
        cont = 0;

        if (instrucoes.isEmpty()) {
            saidas.add("Nenhuma instrução carregada para executar.");
            return;
        }

        while (cont >= 0 && cont < instrucoes.size()) {
            String instrucaoAtual = instrucoes.get(cont);
            int pcAntesDaExecucao = cont;

            String instrucaoBinario = traduzirParaBinario(instrucaoAtual);
            bin.add("Inst. [" + String.format("%03d", pcAntesDaExecucao) + "] (0x" + String.format("%08X", 0x00400000 + pcAntesDaExecucao * 4) + "): " + instrucaoAtual + " -> " + instrucaoBinario);

            if (instrucaoAtual.trim().toLowerCase().startsWith("syscall")) {
                executarSyscall();
                if (cont == pcAntesDaExecucao && registradores.getOrDefault("$v0",0) != 10) {
                     cont++;
                }
            } else {
                executarInstrucao(instrucaoAtual);
                if (cont == pcAntesDaExecucao) {
                    cont++;
                }
            }
        }
        if (cont == instrucoes.size() && (registradores.getOrDefault("$v0",0) != 10 || instrucoes.isEmpty() || !instrucoes.get(Math.max(0,instrucoes.size()-1)).trim().toLowerCase().startsWith("syscall")) ) {
             if (saidas.isEmpty() || !saidas.get(saidas.size()-1).contains("-- Syscall 10: Fim do programa --"))
                saidas.add("-- Fim da execução (fim das instruções) --");
        }
    }

    private void executarSyscall() {
        int v0 = registradores.getOrDefault("$v0", 0);
        switch (v0) {
            case 1:
                saidas.add(String.valueOf(registradores.getOrDefault("$a0", 0)));
                break;
            case 4:
                int enderecoOuHash = registradores.getOrDefault("$a0", 0);
                boolean found = false;
                for (Map.Entry<String, Object> entry : dados.entrySet()) {
                    if (entry.getValue() instanceof String && entry.getKey().hashCode() == enderecoOuHash) {
                        saidas.add((String) entry.getValue());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    saidas.add("<Erro: Syscall 4: Label para $a0 (hash: " + enderecoOuHash + ") não encontrado ou não é .asciiz>");
                }
                break;
            case 5:
                String input = JOptionPane.showInputDialog(null, "Entrada para syscall 5 (ler inteiro):", "Syscall Input", JOptionPane.QUESTION_MESSAGE);
                try {
                    if (input != null) {
                        registradores.put("$v0", Integer.parseInt(input.trim()));
                    } else {
                        registradores.put("$v0", 0);
                        saidas.add("<Syscall 5: Leitura cancelada, $v0 definido como 0>");
                    }
                } catch (NumberFormatException e) {
                    registradores.put("$v0", 0);
                    saidas.add("<Erro: Syscall 5: Entrada inválida ('"+input+"'), $v0 definido como 0>");
                }
                break;
            case 10:
                saidas.add("-- Syscall 10: Fim do programa --");
                cont = instrucoes.size();
                break;
            case 11:
                 char ch = (char) (registradores.getOrDefault("$a0", 0) & 0xFF);
                 saidas.add(String.valueOf(ch));
                 break;
            default:
                saidas.add("<Erro: Syscall com código $v0=" + v0 + " não implementado>");
        }
    }

        private void executarInstrucao(String instrucao) {
        String[] partes = instrucao.replaceAll(",", "").trim().split("\\s+");
        if (partes.length == 0 || partes[0].isEmpty()) return;
        String opcode = partes[0].toLowerCase();

        try {
            switch (opcode) {
                case "add": 
                    registradores.put(partes[1], registradores.get(partes[2]) + registradores.get(partes[3]));
                    break;
                case "addi": 
                    registradores.put(partes[1], registradores.get(partes[2]) + Integer.decode(partes[3]));
                    break;
                case "sub": 
                    registradores.put(partes[1], registradores.get(partes[2]) - registradores.get(partes[3]));
                    break;
                case "and": 
                    registradores.put(partes[1], registradores.get(partes[2]) & registradores.get(partes[3]));
                    break;
                case "or": 
                    registradores.put(partes[1], registradores.get(partes[2]) | registradores.get(partes[3]));
                    break;
                case "sll": 
                    registradores.put(partes[1], registradores.get(partes[2]) << Integer.parseInt(partes[3]));
                    break;
                case "srl": 
                    registradores.put(partes[1], registradores.get(partes[2]) >>> Integer.parseInt(partes[3]));
                    break;
                case "slt": 
                    registradores.put(partes[1], registradores.get(partes[2]) < registradores.get(partes[3]) ? 1 : 0);
                    break;
                case "slti": 
                    registradores.put(partes[1], registradores.get(partes[2]) < Integer.decode(partes[3]) ? 1 : 0);
                    break;
                case "li": 
                    registradores.put(partes[1], Integer.decode(partes[2]));
                    break;
                case "lui":
                    if (partes.length < 3) throw new IllegalArgumentException("Instrução 'lui' requer 2 operandos (rt, immediate).");
                    registradores.put(partes[1], Integer.decode(partes[2]) << 16);
                    break;
                case "la": 
                    String labelLa = partes[2];
                    if (dados.containsKey(labelLa) || vetores.containsKey(labelLa)) {
                        registradores.put(partes[1], labelLa.hashCode());
                    } else if (rotulos.containsKey(labelLa)) {
                        registradores.put(partes[1], rotulos.get(labelLa) * 4 + 0x00400000);
                    } else {
                        throw new IllegalArgumentException("Label '" + labelLa + "' não encontrado para 'la'");
                    }
                    break;
                case "lw": 
                case "sw": 
                    if (partes.length < 3) throw new IllegalArgumentException("Instrução '"+opcode+"' malformada: " + instrucao);
                    String rt = partes[1];
                    String acessoMemoria = partes[2];
                    int offset = 0;
                    String base;

                    if (acessoMemoria.contains("(")) {
                        try {
                            offset = Integer.decode(acessoMemoria.substring(0, acessoMemoria.indexOf('(')));
                            base = acessoMemoria.substring(acessoMemoria.indexOf('(') + 1, acessoMemoria.length() - 1);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Formato de acesso à memória inválido para "+opcode+": " + acessoMemoria + " em '" + instrucao + "'");
                        }
                    } else {
                        base = acessoMemoria;
                        offset = 0;
                    }

                    if (registradores.containsKey(base)) {
                         throw new UnsupportedOperationException(opcode+" com registrador '" + base +"' como base direta não é suportado. Base deve ser um label de dados.");
                    }

                    if (vetores.containsKey(base)) {
                        ArrayList<Integer> vetor = vetores.get(base);
                        if (offset % 4 != 0) throw new IllegalArgumentException(opcode+": offset (" + offset + ") deve ser múltiplo de 4 para acesso a palavras.");
                        int indice = offset / 4;
                        if (indice >= 0 && indice < vetor.size()) {
                            if (opcode.equals("lw")) {
                                registradores.put(rt, vetor.get(indice));
                            } else {
                                if (!registradores.containsKey(rt)) throw new IllegalArgumentException("sw: Registrador fonte '" + rt + "' não encontrado.");
                                vetor.set(indice, registradores.get(rt));
                            }
                        } else {
                            throw new ArrayIndexOutOfBoundsException(opcode+": Acesso fora dos limites ao vetor '" + base + "'.");
                        }
                    } else if (dados.containsKey(base) && dados.get(base) instanceof Integer) {
                        if (offset == 0) {
                            if (opcode.equals("lw")) {
                                registradores.put(rt, (Integer) dados.get(base));
                            } else {
                                if (!registradores.containsKey(rt)) throw new IllegalArgumentException("sw: Registrador fonte '" + rt + "' não encontrado.");
                                dados.put(base, registradores.get(rt));
                            }
                        } else {
                            throw new IllegalArgumentException(opcode+": Offset deve ser 0 para acessar um item .word único ('" + base + "').");
                        }
                    } else {
                        throw new IllegalArgumentException(opcode+": Label '" + base + "' não encontrado como vetor ou dado .word.");
                    }
                    break;
                case "move":
                     if (partes.length < 3) throw new IllegalArgumentException("Instrução 'move' malformada.");
                     if (!registradores.containsKey(partes[1]) || !registradores.containsKey(partes[2])) throw new IllegalArgumentException("Registrador não encontrado para 'move'.");
                     registradores.put(partes[1], registradores.get(partes[2]));
                     break;
                case "j": 
                    if (rotulos.containsKey(partes[1])) {
                        cont = rotulos.get(partes[1]);
                    } else {
                        throw new IllegalArgumentException("Rótulo '" + partes[1] + "' não encontrado para 'j'");
                    }
                    break;
                case "jal":
                    if (partes.length < 2) throw new IllegalArgumentException("Instrução 'jal' requer um label.");
                    if (rotulos.containsKey(partes[1])) {
                        registradores.put("$ra", cont + 1);
                        cont = rotulos.get(partes[1]);
                    } else {
                        throw new IllegalArgumentException("Rótulo '" + partes[1] + "' não encontrado para 'jal'");
                    }
                    break;
                case "jr":
                    if (partes.length < 2) throw new IllegalArgumentException("Instrução 'jr' requer um registrador.");
                    cont = registradores.get(partes[1]);
                    break;
                case "beq": 
                    if (!registradores.containsKey(partes[1]) || !registradores.containsKey(partes[2])) throw new IllegalArgumentException("Registrador não encontrado para 'beq'.");
                    if (registradores.get(partes[1]).equals(registradores.get(partes[2]))) {
                        if (rotulos.containsKey(partes[3])) {
                            cont = rotulos.get(partes[3]);
                        } else {
                            throw new IllegalArgumentException("Rótulo '" + partes[3] + "' não encontrado para 'beq'");
                        }
                    }
                    break;
                case "bne":
                     if (!registradores.containsKey(partes[1]) || !registradores.containsKey(partes[2])) throw new IllegalArgumentException("Registrador não encontrado para 'bne'.");
                     if (!registradores.get(partes[1]).equals(registradores.get(partes[2]))) {
                        if (rotulos.containsKey(partes[3])) {
                            cont = rotulos.get(partes[3]);
                        } else {
                            throw new IllegalArgumentException("Rótulo '" + partes[3] + "' não encontrado para 'bne'");
                        }
                    }
                    break;
                default:
                    String erroMsgDefault = "Instrução não reconhecida ou não implementada: " + opcode + " em '" + instrucao + "'";
                    throw new IllegalArgumentException(erroMsgDefault);
            }
        } catch (Exception e) {
            String erroMsgExec = "Erro ao executar instrução '" + instrucao + "': " + e.getMessage();
            throw new RuntimeException(erroMsgExec, e);
        }
    }


    private String traduzirParaBinario(String instrucao) {
        String[] partes = instrucao.replaceAll(",", "").trim().split("\\s+");
        if (partes.length == 0 || partes[0].isEmpty()) return "VAZIO";
        String opcode = partes[0].toLowerCase();

        Map<String, String> tabelaOpcode = new HashMap<>();
        tabelaOpcode.put("add", "R(add)"); tabelaOpcode.put("addi", "I(addi)");
        tabelaOpcode.put("sub", "R(sub)");
        tabelaOpcode.put("and", "R(and)"); tabelaOpcode.put("or", "R(or)");
        tabelaOpcode.put("sll", "R(sll)");
        tabelaOpcode.put("slt", "R(slt)"); tabelaOpcode.put("slti", "I(slti)");
        tabelaOpcode.put("li", "P(li)"); tabelaOpcode.put("la", "P(la)");
        tabelaOpcode.put("lw", "I(lw)"); tabelaOpcode.put("sw", "I(sw)");
        tabelaOpcode.put("j", "J(j)"); tabelaOpcode.put("beq", "I(beq)");
        tabelaOpcode.put("bne", "I(bne)");
        tabelaOpcode.put("move", "P(move)");
        tabelaOpcode.put("syscall", "Syscall");

        return tabelaOpcode.getOrDefault(opcode, "Desconhecido");
    }

    public void resetar() {
        this.registradores = inicializarRegistradores();
        this.cont = 0;
        this.instrucoes.clear();
        this.saidas.clear();
        this.bin.clear();
        this.rotulos.clear();
        this.dados.clear();
        this.vetores.clear();
        this.saidas.add("Simulador MIPS resetado e pronto.");
    }

    public String obterTextoRegistradores() {
        StringBuilder sb = new StringBuilder("PC (índice): " + cont + " (Endereço: 0x" + String.format("%08X", 0x00400000 + cont * 4) + ")\n");
        List<String> regNomesOrdenados = new ArrayList<>(registradores.keySet());
        regNomesOrdenados.sort(String::compareTo);

        for (String regNome : regNomesOrdenados) {
             Integer valor = registradores.get(regNome);
             sb.append(String.format("%-5s: 0x%08X (%d)\n", regNome, valor, valor));
        }
        return sb.toString();
    }

    public String obterSaidas() {
        if (saidas.isEmpty()) {
            return "Nenhuma saída do programa.";
        }
        return String.join("", saidas);
    }

    public String obterTextoVetores() {
        StringBuilder sb = new StringBuilder();
        if (!vetores.isEmpty()) {
            sb.append("--- Vetores (.word[], .space) ---\n");
            for (Map.Entry<String, ArrayList<Integer>> entry : vetores.entrySet()) {
                sb.append(entry.getKey()).append(":\n  ");
                List<String> valoresFormatados = new ArrayList<>();
                for (Integer val : entry.getValue()) {
                    valoresFormatados.add(String.format("0x%08X", val));
                }
                for (int i = 0; i < valoresFormatados.size(); i++) {
                    sb.append(valoresFormatados.get(i));
                    if ((i + 1) % 4 == 0 && i < valoresFormatados.size() - 1) {
                        sb.append("\n  ");
                    } else if (i < valoresFormatados.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("--- Nenhum vetor (.word[], .space) definido ---\n");
        }

        if (!dados.isEmpty()) {
            sb.append("\n--- Outros Dados (.asciiz, .word único) ---\n");
            for (Map.Entry<String, Object> entry : dados.entrySet()) {
                sb.append(entry.getKey()).append(": ");
                if (entry.getValue() instanceof String) {
                    sb.append("\"").append(((String)entry.getValue()).replace("\n", "\\n")).append("\" (String)");
                } else if (entry.getValue() instanceof Integer) {
                     sb.append(String.format("0x%08X (%d)", (Integer)entry.getValue(), (Integer)entry.getValue()));
                } else {
                    sb.append(entry.getValue().toString());
                }
                sb.append("\n");
            }
        } else {
             sb.append("\n--- Nenhum outro dado (.asciiz, .word) definido ---\n");
        }
        return sb.toString();
    }

    public String obterBinarios() {
        if (bin.isEmpty() && !instrucoes.isEmpty()) {
            return "Tradução binária (simplificada) será gerada durante a execução.";
        }
        if (bin.isEmpty() && instrucoes.isEmpty()){
            return "Nenhuma instrução carregada.";
        }
        return String.join("\n", bin);
    }
    
    public boolean gerarRelatorio(String caminhoArquivo) {
        return gerarRelatorio(caminhoArquivo, null); 
    }

    public boolean gerarRelatorio(String caminhoArquivo, String codigoFonteEditor) {
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(caminhoArquivo))) {
            escritor.write("--- Relatório da Simulação MIPS ---\n\n");

            String codigoParaRelatorio = "Nenhum código MIPS fornecido ou carregado.";
            if (codigoFonteEditor != null && !codigoFonteEditor.trim().isEmpty()) {
                 codigoParaRelatorio = codigoFonteEditor;
                 escritor.write("--- Código MIPS Fornecido (do Editor) ---\n");
            } else if (!instrucoes.isEmpty()){
                 StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw);
                 for(String instr : instrucoes) { pw.println(instr); }
                 codigoParaRelatorio = sw.toString();
                 escritor.write("--- Instruções MIPS Carregadas ---\n");
            }
            escritor.write(codigoParaRelatorio + "\n");

            escritor.write("\n--- Estado dos Registradores (Final) ---\n");
            escritor.write(obterTextoRegistradores());
            escritor.write("\n--- Saídas do Programa ---\n");
            escritor.write(obterSaidas());
            escritor.write("\n--- Dados e Vetores ---\n");
            escritor.write(obterTextoVetores());
            escritor.write("\n--- Tradução Binária (Simplificada) ---\n");
            escritor.write(obterBinarios());
            escritor.write("\n\n--- Fim do Relatório ---");
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao salvar relatório: " + e.getMessage());
            return false;
        }
    }
}
