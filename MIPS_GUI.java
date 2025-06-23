import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

// Classe MIPS_GUI (Conforme fornecido pelo usuário na última interação)
public class MIPS_GUI extends JFrame {
    private SimuladorMIPS32 simulador;

    private JButton botaoCarregar;
    private JButton botaoExecutar;
    private JButton botaoResetar;
    private JButton botaoRelatorio;

    private JTextArea areaEditorCodigo; // Novo: Editor de código MIPS
    private JTextArea areaRegistradores;
    private JTextArea areaBinarios;
    private JTextArea areaSaidas;
    private JTextArea areaDadosVetores;


    public MIPS_GUI() {
        this.simulador = new SimuladorMIPS32();
        setTitle("Simulador MIPS32 - Edição Java com Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800); // Aumentar um pouco o tamanho para o editor
        setLocationRelativeTo(null);

        // --- Painel Superior para Botões (Mantido como antes) ---
        JPanel painelSuperior = new JPanel(new GridBagLayout());
        GridBagConstraints gbcBotoes = new GridBagConstraints();

        botaoCarregar = new JButton("Carregar Arquivo"); // Texto alterado para clareza
        botaoExecutar = new JButton("Executar Código do Editor"); // Texto alterado
        botaoResetar = new JButton("Resetar");
        botaoRelatorio = new JButton("Gerar Relatório");

        gbcBotoes.gridx = 0;
        gbcBotoes.gridy = 0;
        gbcBotoes.weightx = 1.0;
        gbcBotoes.fill = GridBagConstraints.HORIZONTAL;
        painelSuperior.add(Box.createHorizontalGlue(), gbcBotoes);

        gbcBotoes.weightx = 0.0;
        gbcBotoes.fill = GridBagConstraints.NONE;
        gbcBotoes.anchor = GridBagConstraints.CENTER;
        gbcBotoes.insets = new Insets(5, 5, 5, 5);

        gbcBotoes.gridx = 1;
        painelSuperior.add(botaoCarregar, gbcBotoes);
        gbcBotoes.gridx = 2;
        painelSuperior.add(botaoExecutar, gbcBotoes);
        gbcBotoes.gridx = 3;
        painelSuperior.add(botaoResetar, gbcBotoes);
        gbcBotoes.gridx = 4;
        painelSuperior.add(botaoRelatorio, gbcBotoes);

        gbcBotoes.gridx = 5;
        gbcBotoes.weightx = 1.0;
        gbcBotoes.fill = GridBagConstraints.HORIZONTAL;
        gbcBotoes.insets = new Insets(0,0,0,0);
        painelSuperior.add(Box.createHorizontalGlue(), gbcBotoes);

        // --- Área Central com Editor e Saídas ---
        // Novo: Editor de Código MIPS
        areaEditorCodigo = new JTextArea();
        areaEditorCodigo.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Fonte monoespaçada para código
        areaEditorCodigo.setToolTipText("Digite ou cole seu código MIPS aqui. Use .data e .text para as seções.");
        JScrollPane scrollEditor = new JScrollPane(areaEditorCodigo);
        scrollEditor.setBorder(BorderFactory.createTitledBorder("Editor de Código MIPS"));


        // Painel para as áreas de saída (à direita do JSplitPane)
        JPanel painelDireitoSaidas = new JPanel(new GridBagLayout());
        GridBagConstraints gbcAreasTexto = new GridBagConstraints();

        areaRegistradores = new JTextArea("Registradores:\n");
        areaRegistradores.setEditable(false);
        areaRegistradores.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollRegs = new JScrollPane(areaRegistradores);
        scrollRegs.setBorder(BorderFactory.createTitledBorder("Registradores"));


        areaDadosVetores = new JTextArea("Dados e Vetores:\n");
        areaDadosVetores.setEditable(false);
        areaDadosVetores.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollDados = new JScrollPane(areaDadosVetores);
        scrollDados.setBorder(BorderFactory.createTitledBorder("Memória de Dados"));

        areaBinarios = new JTextArea("Binários:\n");
        areaBinarios.setEditable(false);
        areaBinarios.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollBin = new JScrollPane(areaBinarios);
        scrollBin.setBorder(BorderFactory.createTitledBorder("Instruções (Binário Simulado)"));

        areaSaidas = new JTextArea("Saídas do Programa (Syscall):\n");
        areaSaidas.setEditable(false);
        areaSaidas.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollSaidas = new JScrollPane(areaSaidas);
        scrollSaidas.setBorder(BorderFactory.createTitledBorder("Saídas (Console)"));

        // Configurações para as áreas de texto no painelDireitoSaidas
        gbcAreasTexto.fill = GridBagConstraints.BOTH;
        gbcAreasTexto.weightx = 1.0;
        gbcAreasTexto.weighty = 1.0;
        gbcAreasTexto.insets = new Insets(2, 2, 2, 2); // Espaçamento menor

        // Layout 2x2 para as áreas de saída
        gbcAreasTexto.gridx = 0; gbcAreasTexto.gridy = 0;
        painelDireitoSaidas.add(scrollRegs, gbcAreasTexto);
        gbcAreasTexto.gridx = 1; gbcAreasTexto.gridy = 0;
        painelDireitoSaidas.add(scrollDados, gbcAreasTexto);
        gbcAreasTexto.gridx = 0; gbcAreasTexto.gridy = 1;
        painelDireitoSaidas.add(scrollBin, gbcAreasTexto);
        gbcAreasTexto.gridx = 1; gbcAreasTexto.gridy = 1;
        painelDireitoSaidas.add(scrollSaidas, gbcAreasTexto);

        // JSplitPane para dividir editor e saídas
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollEditor, painelDireitoSaidas);
        mainSplitPane.setDividerLocation(500); // Posição inicial do divisor
        mainSplitPane.setResizeWeight(0.4); // Como o espaço é distribuído ao redimensionar

        // Adicionar painéis ao Frame
        add(painelSuperior, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER); // Adiciona o JSplitPane ao centro

        // --- Listeners dos Botões ---
        botaoCarregar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                carregarArquivo();
            }
        });

        botaoExecutar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executarSimulacaoDoEditor();
            }
        });

        botaoResetar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetarSimulacao();
            }
        });

        botaoRelatorio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gerarRelatorio();
            }
        });

        // Inicializar exibição
        atualizarInterface();
        areaEditorCodigo.setText(
            ".data\n" +
            "msg: .asciiz \"Digite um numero: \"\n" +
            "resultado_msg: .asciiz \"\\nO numero digitado foi: \"\n" +
            "array1: .word 10, 20, 30, 40\n" + // Exemplo para lw/sw
            "val1: .word 100\n"+               // Exemplo para lw/sw
            "newline: .asciiz \"\\n\"\n" +
            "\n.text\n" +
            "main:\n" +
            "    li $v0, 4           # syscall para imprimir string\n" +
            "    la $a0, msg         # carrega endereço da mensagem 'msg'\n" +
            "    syscall\n" +
            "\n" +
            "    li $v0, 5           # syscall para ler inteiro\n" +
            "    syscall\n" +
            "    move $s0, $v0       # guarda o inteiro lido em $s0\n" +
            "\n" +
            "    li $v0, 4           # syscall para imprimir string\n" +
            "    la $a0, resultado_msg # carrega endereço da mensagem 'resultado_msg'\n" +
            "    syscall\n" +
            "\n" +
            "    li $v0, 1           # syscall para imprimir inteiro\n" +
            "    move $a0, $s0       # move o inteiro de $s0 para $a0 para impressão\n" +
            "    syscall\n" +
            "\n" +
            "    # Exemplo lw/sw\n" +
            "    lw $t0, array1      # Carrega array1[0] (10) em $t0\n" +
            "    addi $t0, $t0, 5    # $t0 = 15\n" +
            "    sw $t0, 4(array1)   # Salva 15 em array1[1] (originalmente 20)\n" +
            "    lw $t1, val1        # Carrega val1 (100) em $t1\n" +
            "    sw $s0, val1        # Salva o numero digitado em val1\n"+
            "\n" +
            "    li $v0, 4           # syscall para imprimir string (nova linha)\n" +
            "    la $a0, newline\n" +
            "    syscall\n" +
            "\n" +
            "    li $v0, 10          # syscall para terminar o programa\n" +
            "    syscall\n"
        );
        areaEditorCodigo.setCaretPosition(0);
    }

    private void carregarArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Carregar Arquivo MIPS (.txt, .asm, .s)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos MIPS (*.txt, *.asm, *.s)", "txt", "asm", "s"));
        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoSelecionado = fileChooser.getSelectedFile();
            try {
                String content = Files.readString(Paths.get(arquivoSelecionado.getAbsolutePath()), StandardCharsets.UTF_8);
                areaEditorCodigo.setText(content);
                areaEditorCodigo.setCaretPosition(0);

                JOptionPane.showMessageDialog(this, "Arquivo '" + arquivoSelecionado.getName() + "' carregado no editor.", "Arquivo Carregado", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao carregar ou ler arquivo: " + ex.getMessage(), "Erro de Arquivo", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                simulador.resetar();
                atualizarInterface();
            }
        }
    }

    private void executarSimulacaoDoEditor() {
        String codigoDoEditor = areaEditorCodigo.getText();
        if (codigoDoEditor.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Editor de código está vazio. Nada para executar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            simulador.carregarInstrucoesDeString(codigoDoEditor);
            simulador.executar();
            JOptionPane.showMessageDialog(this, "Execução concluída!", "Execução", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao processar código do editor: " + ex.getMessage(), "Erro de Código", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro durante a simulação: " + e.getMessage(), "Erro de Execução", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            atualizarInterface();
        }
    }

    private void resetarSimulacao() {
        simulador.resetar();
        atualizarInterface();
        JOptionPane.showMessageDialog(this, "Simulador resetado.", "Reset", JOptionPane.INFORMATION_MESSAGE);
    }

    private void atualizarInterface() {
        if (simulador != null) {
            areaRegistradores.setText(simulador.obterTextoRegistradores());
            areaSaidas.setText(simulador.obterSaidas());
            areaBinarios.setText(simulador.obterBinarios());
            areaDadosVetores.setText(simulador.obterTextoVetores());
        } else {
            String textoInicial = "Simulador não inicializado.";
            areaRegistradores.setText("Registradores:\n" + textoInicial);
            areaSaidas.setText("Saídas:\n" + textoInicial);
            areaBinarios.setText("Binários (Simplificado):\n" + textoInicial);
            areaDadosVetores.setText("Dados e Vetores:\n" + textoInicial);
        }

        JTextArea[] areas = {areaRegistradores, areaSaidas, areaBinarios, areaDadosVetores};
        for (JTextArea area : areas) {
            if (area != null) area.setCaretPosition(0);
        }
    }

    private void gerarRelatorio() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Relatório como...");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("relatorio_mips_gui.txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoParaSalvar = fileChooser.getSelectedFile();
            if (simulador != null) {
                // Passando o código do editor para o relatório
                boolean sucesso = simulador.gerarRelatorio(arquivoParaSalvar.getAbsolutePath(), areaEditorCodigo.getText());
                if (sucesso) {
                    JOptionPane.showMessageDialog(this, "Relatório salvo em: " + arquivoParaSalvar.getAbsolutePath(), "Relatório Salvo", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Erro ao salvar o relatório.", "Erro de Relatório", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                 JOptionPane.showMessageDialog(this, "Simulador não inicializado. Não é possível gerar relatório.", "Erro de Relatório", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
         try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MIPS_GUI().setVisible(true);
            }
        });
    }
}

// Classe SimuladorMIPS32 (Com lw/sw implementados e compatível com a GUI)
class SimuladorMIPS32 {
    private Map<String, Integer> registradores;
    private int cont; // Contador de Programa (Program Counter - índice da instrução)
    private List<String> instrucoes; // Lista de instruções assembly a serem executadas
    private List<String> saidas; // Saídas do programa (ex: syscalls)
    private List<String> bin; // Representação binária (simplificada) das instruções executadas
    private Map<String, Integer> rotulos; // Mapeia labels para índices de instrução na lista 'instrucoes'
    private Map<String, Object> dados; // Armazena dados da seção .data (ex: .asciiz, .word únicos)
    private Map<String, ArrayList<Integer>> vetores; // Armazena arrays da seção .data (ex: .word[], .space)

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
        String[] regNomes = {
            "$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
            "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
        };
        for (String nome : regNomes) {
            regs.put(nome, 0);
        }
        // regs.put("$sp", 0x7ffffffc); // Exemplo
        return regs;
    }

    public void carregarInstrucoesDeString(String codigoCompleto) throws IOException {
        resetar();
        try (BufferedReader leitor = new BufferedReader(new StringReader(codigoCompleto))) {
            parseCodigo(leitor);
        }
    }
    
    // Mantido para possível uso futuro ou consistência, embora a GUI use DeString
    public void carregarInstrucoes(String nomeArquivo) throws IOException {
        resetar();
        try (BufferedReader leitor = new BufferedReader(new FileReader(nomeArquivo, StandardCharsets.UTF_8))) {
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
                    // System.err.println("Linha " + linhaNum + ": Diretiva '" + linha + "' fora de .data ou .text. Ignorando.");
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
            case 1: // Imprimir inteiro
                saidas.add(String.valueOf(registradores.getOrDefault("$a0", 0)));
                break;
            case 4: // Imprimir string
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
            case 5: // Ler inteiro
                String input = JOptionPane.showInputDialog(null, "Entrada para syscall 5 (ler inteiro):", "Syscall Input", JOptionPane.QUESTION_MESSAGE);
                try {
                    if (input != null) {
                        registradores.put("$v0", Integer.parseInt(input.trim()));
                    } else {
                        registradores.put("$v0", 0); // Cancelado
                        saidas.add("<Syscall 5: Leitura cancelada, $v0 definido como 0>");
                    }
                } catch (NumberFormatException e) {
                    registradores.put("$v0", 0);
                    saidas.add("<Erro: Syscall 5: Entrada inválida ('"+input+"'), $v0 definido como 0>");
                }
                break;
            case 10: // Sair
                saidas.add("-- Syscall 10: Fim do programa --");
                cont = instrucoes.size();
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
                case "slt": 
                    registradores.put(partes[1], registradores.get(partes[2]) < registradores.get(partes[3]) ? 1 : 0);
                    break;
                case "slti": 
                    registradores.put(partes[1], registradores.get(partes[2]) < Integer.decode(partes[3]) ? 1 : 0);
                    break;
                case "li": 
                    registradores.put(partes[1], Integer.decode(partes[2]));
                    break;
                case "la": 
                    String labelLa = partes[2];
                    if (dados.containsKey(labelLa) || vetores.containsKey(labelLa)) {
                        registradores.put(partes[1], labelLa.hashCode());
                    } else if (rotulos.containsKey(labelLa)) {
                        registradores.put(partes[1], rotulos.get(labelLa) * 4 + 0x00400000); // Endereço simulado
                    } else {
                        throw new IllegalArgumentException("Label '" + labelLa + "' não encontrado para 'la'");
                    }
                    break;

                case "lw": 
                    if (partes.length < 3) throw new IllegalArgumentException("Instrução 'lw' malformada: " + instrucao);
                    String rtLw = partes[1];
                    String acessoMemLw = partes[2];
                    int offsetLw = 0;
                    String baseLabelLw;

                    if (acessoMemLw.contains("(")) {
                        try {
                            offsetLw = Integer.decode(acessoMemLw.substring(0, acessoMemLw.indexOf('(')));
                            baseLabelLw = acessoMemLw.substring(acessoMemLw.indexOf('(') + 1, acessoMemLw.length() - 1);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Formato de acesso à memória inválido para lw: " + acessoMemLw + " em '" + instrucao + "'");
                        }
                    } else {
                        baseLabelLw = acessoMemLw;
                        offsetLw = 0;
                    }

                    if (registradores.containsKey(baseLabelLw)) {
                         throw new UnsupportedOperationException("lw com registrador '" + baseLabelLw +"' como base direta não é suportado da forma esperada. Base deve ser um label de dados/vetor.");
                    }

                    if (vetores.containsKey(baseLabelLw)) {
                        ArrayList<Integer> vetor = vetores.get(baseLabelLw);
                        if (offsetLw % 4 != 0) throw new IllegalArgumentException("lw: offset (" + offsetLw + ") deve ser múltiplo de 4 para acesso a palavras em '" + instrucao + "'.");
                        int indice = offsetLw / 4;
                        if (indice >= 0 && indice < vetor.size()) {
                            registradores.put(rtLw, vetor.get(indice));
                        } else {
                            throw new ArrayIndexOutOfBoundsException("lw: Acesso fora dos limites ao vetor '" + baseLabelLw + "' com offset " + offsetLw + " (índice " + indice + "). Tamanho do vetor: " + vetor.size() + " em '" + instrucao + "'.");
                        }
                    } else if (dados.containsKey(baseLabelLw) && dados.get(baseLabelLw) instanceof Integer) {
                        if (offsetLw == 0) {
                            registradores.put(rtLw, (Integer) dados.get(baseLabelLw));
                        } else {
                            throw new IllegalArgumentException("lw: Offset deve ser 0 para acessar um item .word único ('" + baseLabelLw + "'). Offset recebido: " + offsetLw + " em '" + instrucao + "'.");
                        }
                    } else {
                        throw new IllegalArgumentException("lw: Label '" + baseLabelLw + "' não encontrado como vetor ou dado .word em '" + instrucao + "'.");
                    }
                    break;

                case "sw": 
                    if (partes.length < 3) throw new IllegalArgumentException("Instrução 'sw' malformada: " + instrucao);
                    String rtSw = partes[1]; 
                    String acessoMemSw = partes[2];
                    int offsetSw = 0;
                    String baseLabelSw;

                    if (acessoMemSw.contains("(")) {
                         try {
                            offsetSw = Integer.decode(acessoMemSw.substring(0, acessoMemSw.indexOf('(')));
                            baseLabelSw = acessoMemSw.substring(acessoMemSw.indexOf('(') + 1, acessoMemSw.length() - 1);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Formato de acesso à memória inválido para sw: " + acessoMemSw + " em '" + instrucao + "'");
                        }
                    } else {
                        baseLabelSw = acessoMemSw;
                        offsetSw = 0;
                    }
                    
                    if (registradores.containsKey(baseLabelSw)) {
                         throw new UnsupportedOperationException("sw com registrador '" + baseLabelSw +"' como base direta não é suportado da forma esperada. Base deve ser um label de dados/vetor.");
                    }

                    if (vetores.containsKey(baseLabelSw)) {
                        ArrayList<Integer> vetor = vetores.get(baseLabelSw);
                         if (offsetSw % 4 != 0) throw new IllegalArgumentException("sw: offset (" + offsetSw + ") deve ser múltiplo de 4 para acesso a palavras em '" + instrucao + "'.");
                        int indice = offsetSw / 4;
                        if (indice >= 0 && indice < vetor.size()) {
                            if (!registradores.containsKey(rtSw)) throw new IllegalArgumentException("sw: Registrador fonte '" + rtSw + "' não encontrado em '" + instrucao + "'.");
                            vetor.set(indice, registradores.get(rtSw));
                        } else {
                            throw new ArrayIndexOutOfBoundsException("sw: Acesso fora dos limites ao vetor '" + baseLabelSw + "' com offset " + offsetSw + " (índice " + indice + "). Tamanho do vetor: " + vetor.size() + " em '" + instrucao + "'.");
                        }
                    } else if (dados.containsKey(baseLabelSw) && dados.get(baseLabelSw) instanceof Integer) {
                        if (offsetSw == 0) {
                            if (!registradores.containsKey(rtSw)) throw new IllegalArgumentException("sw: Registrador fonte '" + rtSw + "' não encontrado em '" + instrucao + "'.");
                            dados.put(baseLabelSw, registradores.get(rtSw));
                        } else {
                            throw new IllegalArgumentException("sw: Offset deve ser 0 para acessar um item .word único ('" + baseLabelSw + "'). Offset recebido: " + offsetSw + " em '" + instrucao + "'.");
                        }
                    } else {
                        throw new IllegalArgumentException("sw: Label '" + baseLabelSw + "' não encontrado como vetor ou dado .word para escrita em '" + instrucao + "'.");
                    }
                    break;
                
                case "j": 
                    if (rotulos.containsKey(partes[1])) {
                        cont = rotulos.get(partes[1]);
                    } else {
                        throw new IllegalArgumentException("Rótulo '" + partes[1] + "' não encontrado para 'j'");
                    }
                    break;
                case "beq": 
                    if (!registradores.containsKey(partes[1]) || !registradores.containsKey(partes[2])) throw new IllegalArgumentException("Registrador não encontrado para 'beq' em '" + instrucao + "'.");
                    if (registradores.get(partes[1]).equals(registradores.get(partes[2]))) {
                        if (rotulos.containsKey(partes[3])) {
                            cont = rotulos.get(partes[3]);
                        } else {
                            throw new IllegalArgumentException("Rótulo '" + partes[3] + "' não encontrado para 'beq'");
                        }
                    }
                    break;
                case "bne":
                     if (!registradores.containsKey(partes[1]) || !registradores.containsKey(partes[2])) throw new IllegalArgumentException("Registrador não encontrado para 'bne' em '" + instrucao + "'.");
                     if (!registradores.get(partes[1]).equals(registradores.get(partes[2]))) {
                        if (rotulos.containsKey(partes[3])) {
                            cont = rotulos.get(partes[3]);
                        } else {
                            throw new IllegalArgumentException("Rótulo '" + partes[3] + "' não encontrado para 'bne'");
                        }
                    }
                    break;
                case "move":
                     if (partes.length < 3) throw new IllegalArgumentException("Instrução 'move' malformada: " + instrucao);
                     if (!registradores.containsKey(partes[1]) || !registradores.containsKey(partes[2])) throw new IllegalArgumentException("Registrador não encontrado para 'move' em '" + instrucao + "'.");
                     registradores.put(partes[1], registradores.get(partes[2]));
                     break;

                default:
                    String erroMsgDefault = "Instrução não reconhecida ou não implementada: " + opcode + " em '" + instrucao + "'";
                    System.err.println(erroMsgDefault);
                    saidas.add("<ERRO: " + erroMsgDefault + ">");
            }
        } catch (Exception e) {
            String erroMsgExec = "Erro ao executar instrução '" + instrucao + "': " + e.getMessage();
            System.err.println(erroMsgExec);
            saidas.add("<ERRO FATAL: " + erroMsgExec + ". Execução interrompida.>");
            cont = instrucoes.size(); 
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
        // Simple sort, can be improved to match canonical MIPS order if needed
        regNomesOrdenados.sort(String::compareTo);


        for (String regNome : regNomesOrdenados) {
             Integer valor = registradores.get(regNome);
             sb.append(String.format("%-5s: 0x%08X (%d)\n", regNome, valor, valor));
        }
        return sb.toString();
    }

    public String obterSaidas() {
        if (saidas.isEmpty()) return "Nenhuma saída do programa.";
        return String.join("\n", saidas);
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
    
    // Sobrecarga para compatibilidade com a chamada da GUI que pode não passar o código do editor
    public boolean gerarRelatorio(String caminhoArquivo) {
        return gerarRelatorio(caminhoArquivo, null); 
    }

    public boolean gerarRelatorio(String caminhoArquivo, String codigoFonteEditor) {
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(caminhoArquivo, StandardCharsets.UTF_8))) {
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
