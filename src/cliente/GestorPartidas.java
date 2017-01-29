package cliente;


import java.io.StringReader;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class GestorPartidas {

	// URI del recurso que permite acceder al juego
	final private String baseURI = "http://localhost:8080/com.flota.ws/servicios/partidas/";
	Client cliente = null;
	// Para guardar el target que obtendrá con la operación nuevaPartida y que le permitirá jugar la partida creada
	private WebTarget targetPartida = null;


	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor de la clase
	 * Crea el cliente
	 */
	public GestorPartidas()  {
        this.cliente = ClientBuilder.newClient();
	}

	/**
	 * Crea una nueva partida
	 * @param	numFilas	numero de filas del tablero
	 * @param	numColumnas	numero de columnas del tablero
	 * @param	numBarcos	numero de barcos
	 */
	public void nuevaPartida(int numFilas, int numColumnas, int numBarcos)   {

		Response response = cliente.target(baseURI).path("/"+numFilas+"/"+numColumnas+"/"+numBarcos)
			.request().post(Entity.xml(""));

		if (response.getStatus() != 201) throw new RuntimeException("Fallo al crear partida");
		// Obtiene la informació sobre el URI del nuevo recurso partida de la cabecera 'Location' en la respuesta
		String recursoPartida = response.getLocation().toString();
		this.targetPartida = cliente.target(recursoPartida);
		response.close();

		System.out.println("Instancio una nueva partida con id: " + recursoPartida);
	}

	/**
	 * Borra una partida
	 */
	public void borraPartida()   {		
		// Borra la partida mediante un DELETE pasando su identificador en la ruta del URI
		Response response = targetPartida
			.request().delete();
		if (response.getStatus() == 404) { // 404 = NOT_FOUND
			throw new NotFoundException("Partida a borrar no encontrada");
		}	  
		response.close();
	}



	/**
	 * Prueba una casilla y devuelve el resultado
	 * @param	fila	fila de la casilla
	 * @param	columna	columna de la casilla
	 * @return			resultado de la prueba: AGUA, TOCADO, ya HUNDIDO, recien HUNDIDO
	 */
	public int pruebaCasilla( int fila, int columna)   {
		// Actualiza la partida mediante un PUT pasando 
		// el identifiador en la ruta del URI
		// y los datos en la cadena de consulta (query string).
		// Pasamos el cuerpo como un texto vacío porque PUT no admite null como argumento
		Response response = targetPartida.path("casilla/"+fila+","+columna)
			.request().put(Entity.text(""));

		if (response.getStatus() == 404) { // 404 = NOT_FOUND
			throw new NotFoundException("Partida a actualizar no encontrada");
		}
		else {
			String resultado = response.readEntity(String.class);
			response.close();
			return Integer.parseInt(resultado);
			
		}
	}

	/**
	 * Obtiene los datos de un barco.
	 * @param	idBarco	identificador del barco
	 * @return			cadena con informacion sobre el barco "fila#columna#orientacion#tamanyo"
	 */
	public String getBarco( int idBarco) throws NotFoundException   {
		Response response = targetPartida.path("barco/"+idBarco).request(MediaType.TEXT_PLAIN).get();
		
		if (response.getStatus() == 404) { // 404 = NOT_FOUND
			throw new NotFoundException("Barco a leer con id: " + idBarco + " no encontrado");
		}
		else {
			// Lee la cadena con el barco del cuerpo (Entity) del mensaje
			String cadenaBarco = response.readEntity(String.class);
			response.close();
			return cadenaBarco;
		}
	}


	/**
	 * Devuelve la informacion sobre todos los barcos
	 * @return			vector de cadenas con la informacion de cada barco
	 */
	protected String[] getSolucion() {
		String cadena = targetPartida.path("/solucion")
				.request().get(String.class);
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(cadena)));
			return XMLASolucion(doc);
		}
		catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
		}
	}
	
	/**
	 * Convierte procesa un Document XML y lo convierte en la solucion de la partida
	 * @return			vector de cadenas con la informacion de cada barco
	 */
	protected String[] XMLASolucion(Document doc) {
		int numBarcos=0;
		Element root = doc.getDocumentElement();
		if (root.getAttribute("tam") != null && !root.getAttribute("tam").trim().equals(""))
			numBarcos = Integer.valueOf(root.getAttribute("tam"));
		NodeList nodes = root.getChildNodes();
		String[] solucion = new String[numBarcos];
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			if (element.getTagName().equals("barco")) {
				solucion[i] = element.getTextContent();
			}
			else System.out.println("[getSolucion: ] Error en el nombre de la etiqueta");
		}
		return solucion;
	}

} // fin clase
