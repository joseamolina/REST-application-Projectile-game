package cliente;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.ws.rs.NotFoundException;

public class ClienteFlotaWS {

	/**
	 * Implementa el juego 'Hundir la flota' mediante una interfaz grafica (GUI) en el lado clente
	 */

	/** Parametros por defecto de una partida */
	public static final int NUMFILAS=8, NUMCOLUMNAS=8, NUMBARCOS=6;
	public static final String[] vLabels = {"A", "B", "C", "D", "E", "F", "H", "I", "J,", "K"}; //Por si queremos mas o menos dimensiones
	
	private GestorPartidas partida;			//Encargado de gestionar las peticiones y respuestas del servidor.
	private GuiTablero guiTablero;			// El cliente se encarga de crear y modificar la interfaz grafica
	
	/** Atributos de la partida guardados en el juego para simplificar su implementacion */
	private int quedan = NUMBARCOS, disparos = 0;

	/**
	 * Programa principal. Crea y lanza un nuevo juego
	 * @param args
	 */
	public static void main(String[] args) {
		ClienteFlotaWS cliente = new ClienteFlotaWS();
		cliente.ejecuta();
	} // end main

	/**
	 * Lanza una nueva tarea para la hebra event dispatching que crea el auxiliarCliente y dibuja la interfaz grafica: tablero
	 */
	private void ejecuta() {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				partida = new GestorPartidas();
			    guiTablero = new GuiTablero(NUMFILAS, NUMCOLUMNAS);
			    guiTablero.dibujaTablero();
			}
		});
	} // end ejecuta
	
	/******************************************************************************************/
	/*********************  CLASE INTERNA GuiTablero   ****************************************/
	/******************************************************************************************/
	public class GuiTablero {

		private int numFilas, numColumnas;

		private JFrame frame = null;        // Tablero de juego
		private JLabel estado = null;       // Texto en el panel de estado
		private JButton buttons[][] = null; // Botones asociados a las casillas de la partida
		
		/**
         * Constructor de una tablero dadas sus dimensiones
         */
        GuiTablero(int numFilas, int numColumnas) {
            this.numFilas = numFilas;
            this.numColumnas = numColumnas;
        	frame = new JFrame();
        	estado = new JLabel();
        	buttons = new JButton[numFilas][numColumnas];
		}

		/**
		 * Dibuja el tablero de juego y crea la partida inicial
		 */
		private void dibujaTablero() {
			try {
				partida.nuevaPartida(NUMFILAS, NUMCOLUMNAS, NUMBARCOS);
			} catch (RuntimeException ex) {
				System.out.println(ex.getMessage());;
			}
			frame.setContentPane(new Container());
			frame.setLayout(new BorderLayout());
        	anyadeMenu();
        	anyadeGrid(numFilas, numColumnas);
        	anyadePanelEstado("Intentos: "+disparos+"   "+"Barcos restantes: "+quedan);
        	frame.pack();
        	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		} // end dibujaTablero

		/**
		 * Anyade el menu de opciones del juego y le asocia un escuchador
		 */
		private void anyadeMenu() {
			JMenuBar barraMenu = new JMenuBar();
			JMenu menu = new JMenu("Opciones");
			JMenuItem solucion = new JMenuItem("Mostrar solucion");
			JMenuItem nueva = new JMenuItem("Nueva partida");
			JMenuItem salir = new JMenuItem("Salir");
			MenuListener escuchadorMenu = new MenuListener();
			
			solucion.addActionListener(escuchadorMenu);
			nueva.addActionListener(escuchadorMenu);
			salir.addActionListener(escuchadorMenu);
			
			menu.add(solucion);
			menu.add(nueva);
			menu.add(salir);
			barraMenu.add(menu);
            frame.add(barraMenu, BorderLayout.NORTH);
		} // end anyadeMenu

		/**
		 * Anyade el panel con las casillas del mar y sus etiquetas.
		 * Cada casilla sera un boton con su correspondiente escuchador
		 * @param nf	numero de filas
		 * @param nc	numero de columnas
		 */
		private void anyadeGrid(int nf, int nc) {
            GridLayout grid = new GridLayout(nf + 1, nc + 2);
            JPanel panel = new JPanel();
            JLabel label;
            panel.setLayout(grid);
			ButtonListener escuchadorBotones = new ButtonListener();
			panel.add(new JLabel());
			for(int i = 1; i <= nc; i++) {
				label = new JLabel(Integer.toString(i));
				label.setHorizontalAlignment(SwingConstants.CENTER);
				panel.add(label);
			}
			panel.add(new JLabel());
			for(int i = 0; i < nf; i++) {
				label = new JLabel(vLabels[i]);
				label.setHorizontalAlignment(SwingConstants.CENTER);
				panel.add(label);
				for(int j = 0; j< nc; j++) {
					JButton boton = new JButton();
					boton.setBorderPainted(true);
					boton.setOpaque(true);
					boton.setPreferredSize(new Dimension(25,25));
					boton.putClientProperty('F', i);
					boton.putClientProperty('C', j);
					boton.addActionListener(escuchadorBotones);
					buttons[i][j] = boton;
					panel.add(boton);
				}
				label = new JLabel(vLabels[i]);
				label.setHorizontalAlignment(SwingConstants.CENTER);
				panel.add(label);
			}
			frame.add(panel, BorderLayout.CENTER);
		} // end anyadeGrid


		/**
		 * Anyade el panel de estado al tablero
		 * @param cadena	cadena inicial del panel de estado
		 */
		private void anyadePanelEstado(String cadena) {
            frame.add(estado, BorderLayout.SOUTH);
            estado.setHorizontalAlignment(SwingConstants.CENTER);
            cambiaEstado(cadena);
		} // end anyadePanel Estado

		/**
		 * Cambia la cadena mostrada en el panel de estado
		 * @param cadenaEstado	nuevo estado
		 */
        private void cambiaEstado(String cadenaEstado) {
        	estado.setText(cadenaEstado);
		} // end cambiaEstado

		/**
		 * Muestra la solucion de la partida y marca la partida como finalizada
		 */
		private void muestraSolucion() {
			for(int i = 0; i < NUMBARCOS; i++)
				try {
					pintaBarcoHundido(partida.getBarco(i).toString());
				} catch (NotFoundException ex) {
					System.out.println(ex.getMessage());
				}
			for(int i = 0; i < numFilas; i++)
				for(int j = 0; j< numColumnas; j++) {
					if(buttons[i][j].getBackground().equals(Color.RED))
						pintaBoton(buttons[i][j], Color.MAGENTA);
					else pintaBoton(buttons[i][j], Color.CYAN);
				}
		} // end muestraSolucion

		/**
		 * Pinta un barco como hundido en el tablero
		 * @param cadenaBarco cadena con los datos del barco codificados como
		 *                    "filaInicial#columnaInicial#orientacion#tamanyo"
		 */
		private void pintaBarcoHundido(String cadenaBarco) {
			String[] datosBarco = cadenaBarco.split("#");
			int filaInicial = Integer.parseInt(datosBarco[0]);
			int columnaInicial = Integer.parseInt(datosBarco[1]);
			String orientacion = datosBarco[2];
			int tamanyo = Integer.parseInt(datosBarco[3]);
			int contador = 0;
			if(orientacion.equals("H")) 
				for(int columna = columnaInicial; contador < tamanyo; contador++)
					pintaBoton(buttons[filaInicial][columna++], Color.RED);
			else
				for(int fila = filaInicial; contador < tamanyo; contador++)
					pintaBoton(buttons[fila++][columnaInicial], Color.RED);
		} // end pintaBarcoHundido

		/**
		 * Pinta un boton de un color dado
		 * @param b			boton a pintar
		 * @param color		color a usar
		 */
		private void pintaBoton(JButton b, Color color) {
			b.setBackground(color);
			b.setOpaque(true);
		} // end pintaBoton

		/**
		 * Limpia las casillas del tablero pintandolas del gris por defecto
		 */
		private void limpiaTablero() {
			for(int i = 0; i < numFilas; i++)
				for(int j = 0; j< numColumnas; j++)
					pintaBoton(buttons[i][j], null);
		} // end limpiaTablero

		/**
		 * 	Destruye y libera la memoria de todos los componentes del frame
		 */
		private void liberaRecursos() {
			frame.removeAll();
		} // end liberaRecursos

		/**
		 * Desactiva los botones
		 */
		private void desactivaBotones() {
			for(int i = 0; i < numFilas; i++)
				for(int j = 0; j< numColumnas; j++)
					buttons[i][j].setEnabled(false);
		}
		
		/**
		 * Reactiva los botones
		 */
		private void reactivaBotones() {
			for(int i = 0; i < numFilas; i++)
				for(int j = 0; j< numColumnas; j++)
					buttons[i][j].setEnabled(true);
		}
		
	} // end class GuiTablero

	/******************************************************************************************/
	/*********************  CLASE INTERNA MenuListener ****************************************/
	/******************************************************************************************/

	/**
	 * Clase interna que escucha el menu de Opciones del tablero
	 * 
	 */
	private class MenuListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            try {
            	if(menuItem.getText().equals("Mostrar solucion")) {
                	guiTablero.muestraSolucion();
                	guiTablero.desactivaBotones();
                }
                if(menuItem.getText().equals("Nueva partida")) {
                	partida.nuevaPartida(NUMFILAS, NUMCOLUMNAS, NUMBARCOS);
                	disparos = 0;
                	quedan = NUMBARCOS;
                	guiTablero.limpiaTablero();
                	guiTablero.reactivaBotones();
                	guiTablero.cambiaEstado("Intentos: "+disparos+"   "+"Barcos restantes: "+quedan);
                }
                if(menuItem.getText().equals("Salir")) {
                	guiTablero.liberaRecursos();
                	System.exit(0);
                }
			} catch (RuntimeException ex) {
				System.out.println(ex.getMessage());
			}
		} // end actionPerformed

	} // end class MenuListener

	/******************************************************************************************/
	/*********************  CLASE INTERNA ButtonListener **************************************/
	/******************************************************************************************/
	/**
	 * Clase interna que escucha cada uno de los botones del tablero
	 * Para poder identificar el boton que ha generado el evento se pueden usar las propiedades
	 * de los componentes, apoyandose en los metodos putClientProperty y getClientProperty
	 */
	private class ButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
            JButton boton = (JButton) e.getSource();
            int resultadoIntento;
			try {
				resultadoIntento = partida.pruebaCasilla((int)boton.getClientProperty('F'), (int)boton.getClientProperty('C'));
	            disparos++;
	            if(resultadoIntento == -1)
	            	guiTablero.pintaBoton(boton, Color.CYAN);
	            if(resultadoIntento == -2)
	            	guiTablero.pintaBoton(boton, Color.YELLOW);
	            if(resultadoIntento >= 0) {
	            	quedan--;
	            	guiTablero.pintaBarcoHundido(partida.getBarco(resultadoIntento).toString());
	            }
			} catch (RuntimeException ex) {
				System.out.println(ex.getMessage());
			} 
            if(quedan == 0) {
            	guiTablero.cambiaEstado("GAME OVER en "+disparos+" disparos");
            	guiTablero.desactivaBotones();
            }
            else
            	guiTablero.cambiaEstado("Disparos: "+disparos+"   "+"Barcos restantes: "+quedan);
        } // end actionPerformed

	} // end class ButtonListener
	

}