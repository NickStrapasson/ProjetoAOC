# Simulador MIPS em Java

Este é um simulador simples para a arquitetura MIPS32, desenvolvido em Java com uma interface gráfica Swing. Ele permite escrever, carregar e executar código assembly MIPS, visualizando o estado dos registradores e da memória em tempo real.

## Pré-requisitos

- **Java Development Kit (JDK)**: Versão 8 ou superior.

## Como Compilar e Executar

O projeto consiste em dois arquivos principais: `MIPS_GUI.java` e `SimuladorMIPS32.java`. Para utilizá-lo, siga os passos abaixo.

1.  **Salve os Arquivos:**
    Certifique-se de que ambos os arquivos (`MIPS_GUI.java` e `SimuladorMIPS32.java`) estão salvos no mesmo diretório.

2.  **Abra o Terminal (ou Prompt de Comando):**
    Navegue até o diretório onde você salvou os arquivos.
    ```bash
    cd caminho/para/seu/diretorio
    ```

3.  **Compile os Arquivos Java:**
    Execute o comando `javac` para compilar os dois arquivos. Isso gerará os arquivos `.class` necessários.
    ```bash
    javac MIPS_GUI.java SimuladorMIPS32.java
    ```

4.  **Execute o Programa:**
    Após a compilação bem-sucedida, execute a classe principal que contém a interface gráfica.
    ```bash
    java MIPS_GUI
    ```
    A janela do simulador deverá aparecer.

## Como Utilizar a Interface

A interface é dividida em duas seções principais: o editor de código à esquerda e os painéis de visualização à direita.

### 1. Editor de Código

-   **Área de Texto Principal:** Este é o local onde você pode escrever seu código assembly MIPS diretamente.
-   O programa já inicia com um código de exemplo para demonstrar as funcionalidades.

### 2. Painéis de Visualização

-   **Registradores:** Mostra o estado atual de todos os 32 registradores MIPS, incluindo o Program Counter (PC).
-   **Memória de Dados:** Exibe o conteúdo da seção `.data` do seu código, incluindo vetores (`.word[]`, `.space`) e outros dados (`.asciiz`).
-   **Instruções (Binário Simulado):** Lista as instruções da seção `.text` e uma representação simplificada de seu formato binário.
-   **Saídas (Console):** Exibe o resultado das chamadas de sistema (`syscall`), como a impressão de strings e inteiros.

### 3. Botões de Controle

-   **Carregar Arquivo:** Abre uma janela para selecionar um arquivo (`.txt`, `.asm`, `.s`) do seu computador e carregá-lo no editor de código.
-   **Executar Código do Editor:** Analisa e executa o código que está atualmente no editor.
-   **Resetar:** Limpa o estado do simulador, zerando todos os registradores e a memória, e deixando-o pronto para uma nova execução.
-   **Gerar Relatório:** Salva o estado final da simulação (registradores, memória, saídas, etc.) em um arquivo de texto (`.txt`).

## Escrevendo Código Assembly

-   **Seções:** Organize seu código nas seções `.data` (para variáveis) e `.text` (para instruções).
-   **Labels:** Defina labels para dados ou posições de código terminando uma palavra com dois pontos (ex: `main:`, `loop:`, `msg:`).
-   **Comentários:** Use o caractere `#`. Tudo após `#` em uma linha será ignorado.
-   **Instruções Suportadas:** O simulador suporta um conjunto básico de instruções aritméticas (`add`, `addi`, `sub`), lógicas (`and`, `or`, `sll`), de acesso à memória (`lw`, `sw`) e de desvio (`beq`, `bne`, `j`).
-   **Syscalls:** Use `li $v0, <codigo>` seguido de `syscall` para interagir com o simulador (ex: `li $v0, 1` para imprimir inteiro, `li $v0, 4` para imprimir string, `li $v0, 5` para ler inteiro e `li $v0, 10` para terminar o programa).
