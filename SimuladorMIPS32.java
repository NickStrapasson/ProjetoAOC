import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files; // Para ler o arquivo para o editor
import java.nio.file.Paths;  // Para ler o arquivo para o editor
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Classe SimuladorMIPS32 com o novo método carregarInstrucoesDeString
// (Baseada na classe fornecida pelo usuário)
class SimuladorMIPS32 {
    private Map<String, Integer> registradores;
    private int cont; // Contador de Programa (Program Counter)
    private List<String> instrucoes;
    private List<String> saidas;
    private List<String> bin;
    private Map<String, Integer> rotulos;
    private Map<String, Object> dados; // Pode armazenar Integer ou String (para .asciiz)
    private Map<String, ArrayList<Integer>> vetores; // Para arrays .word, arrays .byte, .space

    public SimuladorMIPS32() {
        this.registradores = inicializarRegistradores();
        this.cont = 0;
        this.instrucoes = new ArrayList<>();
        this.saidas = new ArrayList<>();
        this.bin = new ArrayList<>();
        this.rotulos = new HashMap<>();
        this.dados = new HashMap<>();
        this.vetores = new HashMap<>();
        resetar(); // Garante estado inicial limpo
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
        // Valores iniciais específicos se necessário, ex: $sp
        // regs.put("$sp", 0x7ffffffc); // Exemplo de valor inicial para stack pointer
        return regs;
    }

    // Método para carregar instruções de um arquivo (mantido para referência ou uso futuro)
    public void carregarInstrucoes(String nomeArquivo) throws IOException {
        resetar(); // Limpa o estado anterior
        try (BufferedReader leitor = new BufferedReader(new FileReader(nomeArquivo))) {
            parseCodigo(leitor);
        }
    }

    // Novo método para carregar instruções de uma String
    public void carregarInstrucoesDeString(String codigoCompleto) throws IOException {
        resetar(); // Limpa o estado anterior
        try (BufferedReader leitor = new BufferedReader(new StringReader(codigoCompleto))) {
            parseCodigo(leitor);
        }
    }

    // Método auxiliar de parsing, usado por ambos os métodos de carregamento
    private void parseCodigo(BufferedReader leitor) throws IOException {
        String linha;
        String secaoAtual = null; // Começa sem seção definida
        int enderecoInstrucaoAtual = 0; // Endereço relativo dentro da seção .text

        while ((linha = leitor.readLine()) != null) {
            linha = linha.split("#")[0].trim(); // Remove comentários e espaços
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
                // Se uma linha de código aparecer antes de .data ou .text, assumir .text
                // ou lançar um erro se for necessário diretivas explícitas.
                // Para flexibilidade, vamos assumir .text
                if (!linha.startsWith(".")) { // Não é outra diretiva
                     // System.out.println("INFO: Nenhuma seção definida, assumindo .text para: " + linha);
                     secaoAtual = "text";
                } else {
                    // É outra diretiva, mas nenhuma seção principal (.data/.text) foi definida.
                    // Isso pode ser um erro ou uma diretiva global, ignorar por enquanto ou tratar.
                    // System.err.println("Aviso: Diretiva '" + linha + "' encontrada fora das seções .data ou .text.");
                    continue;
                }
            }

            if ("data".equals(secaoAtual)) {
                // Formato esperado: label: .tipo valor(es)
                String[] partes = linha.split(":", 2); // Divide em label e o resto
                String label = null;
                String definicaoDados;

                if (partes.length == 2) {
                    label = partes[0].trim();
                    definicaoDados = partes[1].trim();
                } else {
                    definicaoDados = partes[0].trim(); // Sem label explícito, pode ser um erro ou anônimo (não padrão)
                    // System.err.println("Aviso: Linha de dados sem label explícito: " + linha);
                    // Para este simulador, vamos exigir labels para dados.
                    // Se não for o caso, pode ser necessário ajustar.
                    // Por agora, se não houver label, e a linha não for vazia, pode ser um erro de formato.
                    // No entanto, o código original do usuário não dividia por ':' na seção de dados,
                    // mas sim por espaços, esperando "var .tipo valor". Vamos seguir o original.
                }

                // Revertendo para a lógica de parsing de dados do código original do usuário
                // label: .tipo valor OU var .tipo valor
                partes = linha.split("\\s+", 3); // Divide em até 3 partes por espaços
                if (partes.length >= 2) {
                    String varNome = partes[0];
                    if (varNome.endsWith(":")) { // Se o primeiro token for "label:", remove o ":"
                        varNome = varNome.substring(0, varNome.length() - 1);
                    }
                    // Se não terminar com ':', então o primeiro token é o nome da variável diretamente.

                    String tipo = partes[1];
                    String valorStr = (partes.length > 2) ? partes[2] : "";

                    try {
                        if (".word".equals(tipo) || ".byte".equals(tipo)) {
                            String[] valoresStr = valorStr.split(",");
                            ArrayList<Integer> valoresInt = new ArrayList<>();
                            for (String v : valoresStr) {
                                valoresInt.add(Integer.parseInt(v.trim()));
                            }
                            vetores.put(varNome, valoresInt);
                        } else if (".space".equals(tipo)) {
                            int tamanho = Integer.parseInt(valorStr.trim());
                            ArrayList<Integer> spaceVec = new ArrayList<>(tamanho);
                            for(int i = 0; i < tamanho; i++) spaceVec.add(0); // Inicializa com zeros
                            vetores.put(varNome, spaceVec);
                        } else if (".asciiz".equals(tipo)) {
                            String stringValor = valorStr.trim();
                            if (stringValor.startsWith("\"") && stringValor.endsWith("\"")) {
                                stringValor = stringValor.substring(1, stringValor.length() - 1);
                                stringValor = stringValor.replace("\\n", "\n").replace("\\t", "\t"); // Trata escapes
                            }
                            dados.put(varNome, stringValor);
                        } else { // Assume como padrão um único inteiro se o tipo não for reconhecido
                            dados.put(varNome, Integer.parseInt(valorStr.trim()));
                        }
                    } catch (NumberFormatException e) {
                        throw new IOException("Valor numérico inválido na linha de dados: '" + linha + "'. Detalhe: " + e.getMessage(), e);
                    }
                } else if (!linha.isEmpty()){
                     throw new IOException("Linha de dados mal formatada: '" + linha + "'. Esperado 'label: .tipo valor' ou 'var .tipo valor'.");
                }

            } else if ("text".equals(secaoAtual)) {
                if (linha.endsWith(":")) {
                    String rotuloNome = linha.substring(0, linha.length() - 1).trim();
                    if (!rotuloNome.isEmpty()) {
                        rotulos.put(rotuloNome, enderecoInstrucaoAtual);
                    } else {
                         throw new IOException("Rótulo vazio encontrado na seção .text: '" + linha + "'");
                    }
                } else {
                    instrucoes.add(linha);
                    enderecoInstrucaoAtual++;
                }
            }
        }
    }


    public void executar() {
        saidas.clear(); // Limpa saídas de execuções anteriores
        bin.clear();    // Limpa representações binárias de execuções anteriores
        // Os registradores e o PC (cont) são resetados em resetar() ou no início de carregarInstrucoes...()
        // Se a execução puder ser chamada múltiplas vezes sem recarregar,
        // o PC (cont) deve ser resetado para 0 aqui, e talvez os registradores (exceto $sp, $gp).
        // A lógica atual é que carregarInstrucoes...() chama resetar(), então PC é 0.

        if (instrucoes.isEmpty()) {
            saidas.add("Nenhuma instrução carregada para executar.");
            return;
        }
        cont = 0; // Garante que a execução comece do início das instruções carregadas

        while (cont >= 0 && cont < instrucoes.size()) {
            String instrucaoAtual = instrucoes.get(cont);
            int pcAntesDaExecucao = cont; // Salva o PC para o caso de branch/jump

            // Traduz e registra o binário *antes* da execução da instrução e do incremento do PC
            String instrucaoBinario = traduzirParaBinario(instrucaoAtual);
            bin.add(String.format("0x%08X: %-20s -> %s", pcAntesDaExecucao * 4, instrucaoAtual, instrucaoBinario)); // Endereço simulado

            if (instrucaoAtual.trim().startsWith("syscall")) {
                executarSyscall(); // Trata syscall
                cont++; // Syscall também avança o PC
            } else {
                executarInstrucao(instrucaoAtual); // Executa outras instruções
                // Se a instrução não for um branch/jump que modificou 'cont', incrementa
                if (cont == pcAntesDaExecucao) {
                    cont++;
                }
            }
        }
        if (cont == instrucoes.size()) {
            saidas.add("-- Fim da execução normal --");
        } else if (cont < 0) { // Pode acontecer se um jump for para um endereço inválido (não tratado aqui)
            saidas.add("-- Execução terminada por PC inválido --");
        }
    }

    private void executarSyscall() {
        int v0 = registradores.getOrDefault("$v0", 0);
        switch (v0) {
            case 1: // Imprimir inteiro
                saidas.add(String.valueOf(registradores.getOrDefault("$a0", 0)));
                break;
            case 4: // Imprimir string
                int enderecoOuHash = registradores.getOrDefault("$a0", 0);
                boolean found = false;
                // Tenta encontrar por nome de label (mais robusto que hash para este contexto)
                for (Map.Entry<String, Object> entry : dados.entrySet()) {
                    if (entry.getValue() instanceof String) {
                         // Se $a0 contiver o hash do label (como no código original)
                         if (entry.getKey().hashCode() == enderecoOuHash) {
                            saidas.add((String) entry.getValue());
                            found = true;
                            break;
                         }
                         // Adicionalmente, se $a0 fosse um "ponteiro" para um label conhecido
                         // (não implementado aqui, $a0 teria que ser o nome do label ou um endereço real)
                    }
                }
                // Se não encontrou por hash, e se $a0 fosse um nome de label (não é o caso aqui)
                // if (!found && dados.containsKey(String.valueOf(enderecoOuHash)) && dados.get(String.valueOf(enderecoOuHash)) instanceof String) {
                //    saidas.add((String) dados.get(String.valueOf(enderecoOuHash)));
                //    found = true;
                // }

                if (!found) {
                    saidas.add("<Erro: Syscall 4: Label para $a0 (hash: " + enderecoOuHash + ") não encontrado ou não é .asciiz>");
                }
                break;
            case 5: // Ler inteiro
                String input = JOptionPane.showInputDialog(null, "Entrada para syscall 5 (ler inteiro):", "Syscall Input", JOptionPane.QUESTION_MESSAGE);
                try {
                    registradores.put("$v0", Integer.parseInt(input.trim()));
                } catch (NumberFormatException e) {
                    registradores.put("$v0", 0); // Valor padrão em caso de erro
                    saidas.add("<Erro: Syscall 5: Entrada inválida, $v0 definido como 0>");
                }
                break;
            case 10: // Sair
                // No loop de execução, "break" já acontece se $v0 for 10 na syscall.
                // Para tornar explícito, podemos setar o PC para fora dos limites.
                saidas.add("-- Syscall 10: Fim do programa --");
                cont = instrucoes.size(); // Força o término do loop de execução
                break;
            default:
                saidas.add("<Erro: Syscall com código $v0=" + v0 + " não implementado>");
        }
    }


    private void executarInstrucao(String instrucao) {
        String[] partes = instrucao.replaceAll(",", "").trim().split("\\s+");
        if (partes.length == 0 || partes[0].isEmpty()) return; // Linha vazia ou apenas espaços
        String opcode = partes[0].toLowerCase(); // Normaliza para minúsculas

        try {
            switch (opcode) {
                case "add": // rd, rs, rt
                    registradores.put(partes[1], registradores.get(partes[2]) + registradores.get(partes[3]));
                    break;
                case "addi": // rt, rs, imediato
                    registradores.put(partes[1], registradores.get(partes[2]) + Integer.parseInt(partes[3]));
                    break;
                case "sub": // rd, rs, rt
                    registradores.put(partes[1], registradores.get(partes[2]) - registradores.get(partes[3]));
                    break;
                // case "mult": // rs, rt (HI/LO não implementado, resultado em $v0 e $v1 por convenção simples)
                //     long resMult = (long)registradores.get(partes[1]) * registradores.get(partes[2]);
                //     registradores.put("$lo", (int)(resMult & 0xFFFFFFFFL));
                //     registradores.put("$hi", (int)(resMult >> 32));
                //     break;
                case "and": // rd, rs, rt
                    registradores.put(partes[1], registradores.get(partes[2]) & registradores.get(partes[3]));
                    break;
                case "or": // rd, rs, rt
                    registradores.put(partes[1], registradores.get(partes[2]) | registradores.get(partes[3]));
                    break;
                case "sll": // rd, rt, shamt
                    registradores.put(partes[1], registradores.get(partes[2]) << Integer.parseInt(partes[3]));
                    break;
                case "srl": // rd, rt, shamt
                    registradores.put(partes[1], registradores.get(partes[2]) >>> Integer.parseInt(partes[3])); // Logical right shift
                    break;
                case "slt": // rd, rs, rt
                    registradores.put(partes[1], registradores.get(partes[2]) < registradores.get(partes[3]) ? 1 : 0);
                    break;
                case "slti": // rt, rs, imediato
                    registradores.put(partes[1], registradores.get(partes[2]) < Integer.parseInt(partes[3]) ? 1 : 0);
                    break;
                case "li": // rt, imediato (pseudo-instrução)
                    registradores.put(partes[1], Integer.parseInt(partes[2]));
                    break;
                case "la": // rt, label (pseudo-instrução)
                    String labelLa = partes[2];
                    if (dados.containsKey(labelLa) || vetores.containsKey(labelLa)) {
                        // A lógica original usava label.hashCode(). Isso é uma simplificação.
                        // Em um simulador real, 'la' carrega um ENDEREÇO.
                        // Para manter a compatibilidade com a lógica de syscall 4:
                        registradores.put(partes[1], labelLa.hashCode());
                        // Se fosse para carregar um endereço de memória simulado:
                        // if (rotulosMemoriaDados.containsKey(labelLa)) {
                        //    registradores.put(partes[1], rotulosMemoriaDados.get(labelLa));
                        // } else { throw new IllegalArgumentException("Label '" + labelLa + "' não encontrado para la.");}
                    } else {
                        throw new IllegalArgumentException("Label '" + labelLa + "' não encontrado no segmento de dados para 'la'");
                    }
                    break;
                case "lw": // rt, offset(baseLabelOuReg)
                case "sw": // rt, offset(baseLabelOuReg)
                    String rt = partes[1];
                    String acessoMemoria = partes[2]; // Ex: "0($s0)" ou "meuArray" ou "4(meuArray)"
                    int offset = 0;
                    String base;

                    if (acessoMemoria.contains("(")) {
                        offset = Integer.parseInt(acessoMemoria.substring(0, acessoMemoria.indexOf('(')));
                        base = acessoMemoria.substring(acessoMemoria.indexOf('(') + 1, acessoMemoria.length() - 1);
                    } else { // Acesso direto por label, ex: lw $t0, var1 (offset é 0)
                        base = acessoMemoria;
                        offset = 0;
                    }

                    int enderecoBaseCalculado;
                    ArrayList<Integer> vetorAlvo = null;

                    if (registradores.containsKey(base)) { // Base é um registrador
                        enderecoBaseCalculado = registradores.get(base);
                        // Neste simulador simplificado, se base é um registrador, ele deve conter um "endereço"
                        // que aponta para o INÍCIO de um vetor nomeado em 'vetores'.
                        // Precisaríamos de um mapeamento reverso ou iterar para encontrar qual vetor.
                        // Isso é complexo. A versão original do usuário parecia focar em 'base' sendo um label.
                        throw new UnsupportedOperationException("lw/sw com registrador base ("+base+") para endereçamento genérico de memória não é totalmente suportado. Use label base.");
                    } else if (vetores.containsKey(base)) { // Base é um label de vetor
                        vetorAlvo = vetores.get(base);
                        enderecoBaseCalculado = 0; // Offset é relativo ao início deste vetor nomeado
                    } else if (dados.containsKey(base) && dados.get(base) instanceof Integer && offset == 0) {
                        // Acesso a um item .word único em 'dados'
                        if (opcode.equals("lw")) {
                            registradores.put(rt, (Integer)dados.get(base));
                        } else { // sw
                            dados.put(base, registradores.get(rt));
                        }
                        return; // Instrução concluída
                    }
                    else {
                        throw new IllegalArgumentException("Label base '" + base + "' para "+opcode+" não encontrado ou tipo inválido.");
                    }

                    // Se chegou aqui, base é um label de vetor em 'vetores'
                    int indiceNoVetor = (enderecoBaseCalculado + offset) / 4; // Assumindo words (4 bytes)

                    if (vetorAlvo == null) throw new InternalError("vetorAlvo não deveria ser nulo aqui");

                    if (indiceNoVetor >= 0 && indiceNoVetor < vetorAlvo.size()) {
                        if (opcode.equals("lw")) {
                            registradores.put(rt, vetorAlvo.get(indiceNoVetor));
                        } else { // sw
                            vetorAlvo.set(indiceNoVetor, registradores.get(rt));
                        }
                    } else {
                        throw new ArrayIndexOutOfBoundsException("Acesso à memória ("+opcode+") fora dos limites para o vetor '" + base + "' no índice " + indiceNoVetor);
                    }
                    break;
                case "move": // rt, rs (pseudo-instrução: addi rt, rs, 0)
                    registradores.put(partes[1], registradores.get(partes[2]));
                    break;
                case "j": // label
                    if (rotulos.containsKey(partes[1])) {
                        cont = rotulos.get(partes[1]); // Define o PC para o endereço do rótulo
                        // Subtrai 1 porque o loop principal incrementará o PC
                        // cont--; // Não, o loop principal não incrementa se o PC mudou. Ajustar loop.
                        // O loop de execução já lida com isso: se cont != pcAntesDaExecucao, não incrementa.
                    } else {
                        throw new IllegalArgumentException("Rótulo '" + partes[1] + "' não encontrado para 'j'");
                    }
                    break;
                case "beq": // rs, rt, label
                case "bne": // rs, rt, label
                    boolean condicao = false;
                    int valRs = registradores.get(partes[1]);
                    int valRt = registradores.get(partes[2]);
                    if (opcode.equals("beq")) {
                        condicao = (valRs == valRt);
                    } else { // bne
                        condicao = (valRs != valRt);
                    }

                    if (condicao) {
                        if (rotulos.containsKey(partes[3])) {
                            cont = rotulos.get(partes[3]);
                            // cont--; // Mesma lógica do 'j'
                        } else {
                            throw new IllegalArgumentException("Rótulo '" + partes[3] + "' não encontrado para '" + opcode + "'");
                        }
                    }
                    break;
                default:
                    // Não lança exceção, apenas registra no log de saídas/binários
                    String erroMsg = "Instrução não reconhecida ou não implementada: " + opcode;
                    System.err.println(erroMsg);
                    saidas.add("<ERRO: " + erroMsg + ">");
                    // bin.add("ERRO: " + instrucao + " -> " + erroMsg); // Já adicionado antes
            }
        } catch (Exception e) {
            String erroMsg = "Erro ao executar instrução '" + instrucao + "': " + e.getMessage();
            System.err.println(erroMsg);
            e.printStackTrace(); // Para depuração no console
            saidas.add("<ERRO FATAL: " + erroMsg + ". Execução interrompida.>");
            cont = instrucoes.size(); // Interrompe a execução
        }
    }

    private String traduzirParaBinario(String instrucao) {
        // Esta é uma tradução MUITO simplificada, apenas para dar uma ideia.
        // Uma tradução real MIPS é complexa.
        String[] partes = instrucao.replaceAll(",", "").trim().split("\\s+");
        String opcode = partes[0].toLowerCase();

        // Tabela de opcodes (alguns exemplos) - Formato MIPS (6 bits)
        Map<String, String> op = new HashMap<>();
        op.put("add",    "000000"); op.put("sub",    "000000"); op.put("and", "000000");
        op.put("or",     "000000"); op.put("slt",    "000000"); op.put("sll", "000000");
        op.put("srl",    "000000");
        op.put("addi",   "001000"); op.put("slti",   "001010");
        op.put("lw",     "100011"); op.put("sw",     "101011");
        op.put("beq",    "000100"); op.put("bne",    "000101");
        op.put("j",      "000010");
        op.put("syscall","000000"); // funct 001100

        // Tabela de funct para tipo R (6 bits)
        Map<String, String> funct = new HashMap<>();
        funct.put("add", "100000"); funct.put("sub", "100010"); funct.put("and", "100100");
        funct.put("or",  "100101"); funct.put("slt", "101010"); funct.put("sll", "000000");
        funct.put("srl", "000010");
        funct.put("syscall", "001100");

        String binOp = op.getOrDefault(opcode, "??????");
        String binRs = "?????"; String binRt = "?????"; String binRd = "?????";
        String binShamt = "?????"; String binFunct = "??????";
        String binImm = "????????????????"; // 16 bits
        String binAddr = "??????????????????????????"; // 26 bits

        try {
            if (Arrays.asList("add", "sub", "and", "or", "slt").contains(opcode)) { // R-Type: opcode rd, rs, rt
                binRd = String.format("%5s", Integer.toBinaryString(getRegNum(partes[1]))).replace(' ', '0');
                binRs = String.format("%5s", Integer.toBinaryString(getRegNum(partes[2]))).replace(' ', '0');
                binRt = String.format("%5s", Integer.toBinaryString(getRegNum(partes[3]))).replace(' ', '0');
                binShamt = "00000";
                binFunct = funct.get(opcode);
                return binOp + "_" + binRs + "_" + binRt + "_" + binRd + "_" + binShamt + "_" + binFunct;
            } else if (Arrays.asList("sll", "srl").contains(opcode)) { // R-Type (shift): opcode rd, rt, shamt
                binRd = String.format("%5s", Integer.toBinaryString(getRegNum(partes[1]))).replace(' ', '0');
                binRt = String.format("%5s", Integer.toBinaryString(getRegNum(partes[2]))).replace(' ', '0');
                binShamt = String.format("%5s", Integer.toBinaryString(Integer.parseInt(partes[3]))).replace(' ', '0');
                binRs = "00000"; // rs não usado
                binFunct = funct.get(opcode);
                return binOp + "_" + binRs + "_" + binRt + "_" + binRd + "_" + binShamt + "_" + binFunct;
            } else if (Arrays.asList("addi", "slti").contains(opcode)) { // I-Type: opcode rt, rs, imm
                binRt = String.format("%5s", Integer.toBinaryString(getRegNum(partes[1]))).replace(' ', '0');
                binRs = String.format("%5s", Integer.toBinaryString(getRegNum(partes[2]))).replace(' ', '0');
                short imm = Short.parseShort(partes[3]);
                binImm = String.format("%16s", Integer.toBinaryString(imm & 0xFFFF)).replace(' ', '0');
                return binOp + "_" + binRs + "_" + binRt + "_" + binImm;
            } else if (Arrays.asList("lw", "sw", "beq", "bne").contains(opcode)) { // I-Type (mem/branch): opcode rt, offset(rs) ou rs, rt, label
                if (Arrays.asList("lw", "sw").contains(opcode)) { // lw rt, offset(rs)
                    binRt = String.format("%5s", Integer.toBinaryString(getRegNum(partes[1]))).replace(' ', '0');
                    String memAccess = partes[2]; // "offset(reg)"
                    int offsetVal = Integer.parseInt(memAccess.substring(0, memAccess.indexOf('(')));
                    String regBase = memAccess.substring(memAccess.indexOf('(') + 1, memAccess.length() - 1);
                    binRs = String.format("%5s", Integer.toBinaryString(getRegNum(regBase))).replace(' ', '0');
                    binImm = String.format("%16s", Integer.toBinaryString(offsetVal & 0xFFFF)).replace(' ', '0');
                } else { // beq rs, rt, label
                    binRs = String.format("%5s", Integer.toBinaryString(getRegNum(partes[1]))).replace(' ', '0');
                    binRt = String.format("%5s", Integer.toBinaryString(getRegNum(partes[2]))).replace(' ', '0');
                    int targetAddr = rotulos.getOrDefault(partes[3], -1);
                    if (targetAddr != -1) {
                        int offsetBranch = (targetAddr - (this.cont +1)); // Offset relativo em words
                        binImm = String.format("%16s", Integer.toBinaryString(offsetBranch & 0xFFFF)).replace(' ', '0');
                    } else {
                        binImm = "LABEL_NOT_FOUND_????????";
                    }
                }
                return binOp + "_" + binRs + "_" + binRt + "_" + binImm;
            } else if (opcode.equals("j")) { // J-Type: opcode address
                int targetAddr = rotulos.getOrDefault(partes[1], -1);
                if (targetAddr != -1) {
                    // Endereço é em words, precisa ser alinhado e os 4 bits mais altos do PC atual
                    // Aqui, simplificamos para o índice da instrução.
                    binAddr = String.format("%26s", Integer.toBinaryString(targetAddr & 0x03FFFFFF)).replace(' ', '0');
                } else {
                    binAddr = "LABEL_NOT_FOUND_??????????";
                }
                return binOp + "_" + binAddr;
            } else if (opcode.equals("syscall")) {
                return binOp + "_00000_00000_00000_00000_" + funct.get("syscall");
            } else if (Arrays.asList("li", "la", "move").contains(opcode)) {
                return "PSEUDO-INSTRUÇÃO (" + instrucao + ")";
            }
        } catch (Exception e) {
            // System.err.println("Erro ao traduzir para binário (simplificado): " + instrucao + " -> " + e.getMessage());
            return "ERRO_NA_TRADUCAO_SIMPLIFICADA";
        }
        return "NAO_MAPEADO_PARA_BINARIO_SIMPLES";
    }

    private int getRegNum(String regName) {
        // Remove '$' e converte para número. Ex: "$t0" -> 8
        // Esta é uma simplificação. Uma tabela de mapeamento seria melhor.
        if (regName.equals("$zero")) return 0; if (regName.equals("$at")) return 1;
        if (regName.startsWith("$v")) return 2 + Integer.parseInt(regName.substring(2));
        if (regName.startsWith("$a")) return 4 + Integer.parseInt(regName.substring(2));
        if (regName.startsWith("$t")) {
            int num = Integer.parseInt(regName.substring(2));
            return (num <= 7) ? (8 + num) : (24 + (num - 8)); // $t0-$t7, $t8-$t9
        }
        if (regName.startsWith("$s")) return 16 + Integer.parseInt(regName.substring(2));
        if (regName.startsWith("$k")) return 26 + Integer.parseInt(regName.substring(2));
        if (regName.equals("$gp")) return 28; if (regName.equals("$sp")) return 29;
        if (regName.equals("$fp")) return 30; if (regName.equals("$ra")) return 31;
        throw new IllegalArgumentException("Nome de registrador inválido para getRegNum: " + regName);
    }


    public void resetar() {
        this.registradores = inicializarRegistradores();
        this.cont = 0; // PC para o início
        this.instrucoes.clear();
        this.saidas.clear();
        this.bin.clear();
        this.rotulos.clear();
        this.dados.clear();
        this.vetores.clear();
        // Adiciona uma mensagem inicial às saídas após o reset
        this.saidas.add("Simulador MIPS resetado e pronto.");
    }

    public String obterTextoRegistradores() {
        StringBuilder sb = new StringBuilder("PC: 0x" + String.format("%08X", cont * 4) + " (índice: "+cont+")\n"); // PC em bytes
        for (Map.Entry<String, Integer> entry : registradores.entrySet()) {
            sb.append(String.format("%-5s: 0x%08X (%d)\n", entry.getKey(), entry.getValue(), entry.getValue()));
        }
        return sb.toString();
    }

    public String obterSaidas() {
        StringBuilder sb = new StringBuilder();
        for (String saida : saidas) {
            sb.append(saida).append("\n");
        }
        return sb.toString().trim(); // Remove nova linha final se houver
    }

    public String obterTextoVetores() {
        StringBuilder sb = new StringBuilder();
        if (!vetores.isEmpty()) {
            sb.append("--- Vetores (.word[], .byte[], .space) ---\n");
            for (Map.Entry<String, ArrayList<Integer>> entry : vetores.entrySet()) {
                sb.append(entry.getKey()).append(":\n  ");
                List<String> valoresHex = new ArrayList<>();
                for(Integer val : entry.getValue()){
                    valoresHex.add(String.format("0x%08X", val));
                }
                // Para melhor visualização, mostrar alguns por linha
                for(int i=0; i<valoresHex.size(); i++){
                    sb.append(valoresHex.get(i));
                    if((i+1) % 4 == 0 && i < valoresHex.size()-1) { // 4 valores por linha
                        sb.append("\n  ");
                    } else if (i < valoresHex.size()-1){
                        sb.append(", ");
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("--- Nenhum vetor definido ---\n");
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
             sb.append("\n--- Nenhum outro dado definido ---\n");
        }
        return sb.toString();
    }

    public String obterBinarios() {
        if (bin.isEmpty() && !instrucoes.isEmpty()) {
            return "Tradução binária será gerada durante a execução.";
        }
        if (bin.isEmpty() && instrucoes.isEmpty()){
            return "Nenhuma instrução carregada.";
        }
        StringBuilder sb = new StringBuilder();
        for (String b : bin) {
            sb.append(b).append("\n");
        }
        return sb.toString().trim();
    }

    public boolean gerarRelatorio(String caminhoArquivo) {
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(caminhoArquivo))) {
            escritor.write("--- Relatório da Simulação MIPS ---\n\n");
            escritor.write("--- Código MIPS Executado (do Editor) ---\n");
            // Se você tiver o código do editor disponível aqui, seria bom incluí-lo.
            // Por agora, vamos pular isso ou você pode passá-lo como parâmetro.
            // Ou, se 'instrucoes' for sempre o código do editor, podemos usá-lo.
            if (!instrucoes.isEmpty()) {
                for(String instr : instrucoes) {
                    escritor.write(instr + "\n");
                }
            } else {
                escritor.write("Nenhum código foi carregado/executado recentemente.\n");
            }
            escritor.write("\n\n--- Estado dos Registradores (Final) ---\n");
            escritor.write(obterTextoRegistradores());
            escritor.write("\n\n--- Saídas do Programa (Syscall) ---\n");
            escritor.write(obterSaidas());
            escritor.write("\n\n--- Memória de Dados (Vetores e Outros) ---\n");
            escritor.write(obterTextoVetores());
            escritor.write("\n\n--- Instruções e Tradução Binária (Simplificada) ---\n");
            escritor.write(obterBinarios());
            escritor.write("\n\n--- Fim do Relatório ---");
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao salvar relatório: " + e.getMessage());
            return false;
        }
    }
}