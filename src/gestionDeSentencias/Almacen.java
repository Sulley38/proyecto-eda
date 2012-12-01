package gestionDeSentencias;

import java.io.*;
import java.util.*;
import estructurasDeDatos.*;

/**
 * Almac�n de sentencias:
 * Contiene todas las sentencias de un fichero y las operaciones definidas para trabajar sobre ellas.
 * @author Daniel, Iv�n, Asier
 */
public class Almacen {

	/**
	 * Estructura de arista del grafo.
	 * Indica el v�rtice ligado, su peso (la propiedad) y el n�mero de veces que aparece repetida.
	 */
	private class Arista implements Comparable<Arista> {
		// Atributos
		private final int verticeObjetivo, propiedad;
		private int repeticiones;
		// Constructora
		public Arista(final int Objetivo, final int Propiedad) {
			verticeObjetivo = Objetivo;
			propiedad = Propiedad;
			repeticiones = 1;
		}
		// Comparadora en orden lexicogr�fico seg�n propiedades y objetos
		@Override
		public int compareTo(final Arista a) {
			if( this.propiedad == a.propiedad )
				return listaSujetosObjetos.get(this.verticeObjetivo).compareTo(listaSujetosObjetos.get(a.verticeObjetivo));
			else
				return listaPropiedades.get(this.propiedad).compareTo(listaPropiedades.get(a.propiedad));
		}
		// Comparadora de igualdad de la arista
		@Override
		public boolean equals(final Object a) {
			return (verticeObjetivo == ((Arista)a).verticeObjetivo) && (propiedad == ((Arista)a).propiedad);
		}
	}
	
	/// CONSTANTES
	private static final String propiedadEs = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
	private static final String propiedadSubClaseDe = "<http://www.w3.org/2000/01/rdf-schema#subClassOf>";
	
	/// ATRIBUTOS DE LA CLASE
	// Dos listas de adyacencia para representar el grafo.
	private ListaArray< ListaArray<Arista> > nodosEntrantes, nodosSalientes;
	// Para cada nodo, �ndices de las aristas siguiendo el orden lexicogr�fico del enunciado
	private ListaArray< ListaArray<Integer> > nodosSalientesOrdenados;
	// N�mero de nodos (sujetos+objetos), de aristas (propiedades) y de sentencias; la suma de los tres primeros es el n�mero de entidades
	private int sujetos, objetos, propiedades, sentencias;
	// Relaciones entre entidad e �ndice correspondiente (Trie), y viceversa (array)
	private Trie arbolSujetosObjetos, arbolPropiedades;
	private ListaArray<String> listaSujetosObjetos, listaPropiedades;
	// Propiedades para trabajar con las clases/subclases/superclases
	private int identificadorPropiedadEs, identificadorPropiedadSubClaseDe;
	

	/**
	 * Constructora
	 * Carga las sentencias en el almac�n desde el fichero especificado.
	 * @param nombreDeArchivo de texto desde el que leer las entidades
	 */
	public Almacen( String nombreDeArchivo ) throws IOException {
		// Inicializa los atributos de la clase
		nodosEntrantes = new ListaArray< ListaArray<Arista> >();
		nodosSalientes = new ListaArray< ListaArray<Arista> >();
		sujetos = objetos = propiedades = sentencias = 0;
		arbolSujetosObjetos = new Trie();
		arbolPropiedades = new Trie();
		listaSujetosObjetos = new ListaArray<String>();
		listaPropiedades = new ListaArray<String>();
		// Variable temporal para insertar las sentencias
		int tempArista;
		// Lee las sentencias desde el fichero y las a�ade al trie y a la lista de nodos del grafo
		Fichero.abrir(nombreDeArchivo,false,false);
		String sentencia, sujeto, propiedad, objeto;
		int idSujeto, idPropiedad, idObjeto;
		StringTokenizer tokenizador;
		while( (sentencia = Fichero.leerSentencia()) != null ) {
			tokenizador = new StringTokenizer(sentencia);
			sujeto = tokenizador.nextToken();
			propiedad = tokenizador.nextToken();
			objeto = tokenizador.nextToken();
			sentencias++;
			
			// Insertar sujeto en el trie
			idSujeto = arbolSujetosObjetos.insertar(sujeto, sujetos+objetos);
			if( idSujeto == sujetos+objetos ) {
				// Agregar al array de int -> String
				listaSujetosObjetos.insertLast(sujeto);
				// A�adir su hueco en las listas de adyacencia
				nodosEntrantes.insertLast( new ListaArray<Arista>() );
				nodosSalientes.insertLast( new ListaArray<Arista>() );
				// Incrementar contador
				sujetos++;
			}
			// Insertar propiedad en el trie
			idPropiedad = arbolPropiedades.insertar(propiedad, propiedades);
			if( idPropiedad == propiedades ) {
				// Agregar al array de int -> String
				listaPropiedades.insertLast(propiedad);
				// En caso de ser una propiedad especial, guardar su entero
				if( propiedad.equals(propiedadEs) )
					identificadorPropiedadEs = idPropiedad;
				else if( propiedad.equals(propiedadSubClaseDe) )
					identificadorPropiedadSubClaseDe = idPropiedad;
				// Incrementar contador
				propiedades++;
			}
			// Insertar objeto en el trie
			idObjeto = arbolSujetosObjetos.insertar(objeto, sujetos+objetos);
			if( idObjeto == sujetos+objetos ) {
				// Agregar al array de int -> String
				listaSujetosObjetos.insertLast(objeto);
				// A�adir su hueco en las listas de adyacencia
				nodosEntrantes.insertLast( new ListaArray<Arista>() );
				nodosSalientes.insertLast( new ListaArray<Arista>() );
				// Incrementar contador
				objetos++;
			}
			
			// Inserta la arista en la primera lista de adyacencia, o a�ade una repetici�n
			tempArista = nodosSalientes.get(idSujeto).find( new Arista(idObjeto,idPropiedad) );
			if( tempArista == -1 )
				nodosSalientes.get(idSujeto).insertLast( new Arista(idObjeto,idPropiedad) );
			else
				nodosSalientes.get(idSujeto).get(tempArista).repeticiones++;
			
			// Inserta la arista en la segunda lista de adyacencia, o a�ade una repetici�n
			tempArista = nodosEntrantes.get(idObjeto).find( new Arista(idSujeto,idPropiedad) );
			if( tempArista == -1 )
				nodosEntrantes.get(idObjeto).insertLast( new Arista(idSujeto,idPropiedad) );
			else
				nodosEntrantes.get(idObjeto).get(tempArista).repeticiones++;
		}
		
		// Ordenar las aristas salientes de cada nodo
		nodosSalientesOrdenados = new ListaArray< ListaArray<Integer> >(nodosSalientes.size());
		for( int i = 0; i < nodosSalientes.size(); ++i )
			nodosSalientesOrdenados.set( i, nodosSalientes.get(i).sort() );

		Fichero.cerrar();
	}
	
	/**
	 * 1) Colecci�n de sentencias del almac�n que tienen un sujeto determinado.
	 * @param sujeto - sujeto cuyas sentencias han de devolverse
	 * @return Una lista enlazada de sentencias que tienen Sujeto como sujeto
	 */
	public ListaEnlazada<String> sentenciasPorSujeto( String sujeto ) {	
		ListaEnlazada<String> coleccionSentencias = new ListaEnlazada<String>();
		int index = arbolSujetosObjetos.obtenerValor(sujeto);
		if( index != -1 ) {
			// Si el sujeto no existe, devuelve una lista vac�a
			Arista prov;
			for( int i = 0; i < nodosSalientes.get(index).size(); ++i ) {
				prov = nodosSalientes.get(index).get(i);
				for( int j = 0; j < prov.repeticiones; j++ )
					coleccionSentencias.insertLast( listaSujetosObjetos.get(index) + " " +  listaPropiedades.get(prov.propiedad) + " " + listaSujetosObjetos.get(prov.verticeObjetivo) + " ." );				
			}
		}
		return coleccionSentencias;
	}
	
	/**
	 * 2) Colecci�n de sentencias distintas del almac�n que tienen un sujeto determinado.
	 * @param sujeto - sujeto cuyas sentencias han de devolverse
	 * @return Una lista enlazada de sentencias sin repeticiones que tienen Sujeto como sujeto
	 */
	public ListaEnlazada<String> sentenciasDistintasPorSujeto( String sujeto ) {
		ListaEnlazada<String> coleccionSentencias = new ListaEnlazada<String>();
		int index = arbolSujetosObjetos.obtenerValor(sujeto);
		if( index != -1 ) {
			// Si el sujeto no existe, devuelve una lista vac�a
			Arista prov;
			for( int i = 0; i < nodosSalientes.get(index).size(); ++i ) {
				prov = nodosSalientes.get(index).get(i);
				coleccionSentencias.insertLast( listaSujetosObjetos.get(index) + " " +  listaPropiedades.get(prov.propiedad) + " " + listaSujetosObjetos.get(prov.verticeObjetivo) + " ." );
			}
		}
		return coleccionSentencias;
	}
	
	/**
	 * 3) Colecci�n de propiedades distintas que aparecen en las sentencias del almac�n.
	 * @return Un array de las propiedades sin repeticiones que aparecen en el almac�n
	 */
	public ListaArray<String> propiedadesDistintas() {
		return listaPropiedades;
	}
	
	/**
	 * 4) Colecci�n de entidades distintas que son sujeto de alguna sentencia y tambi�n
	 * 	  son objeto de alguna sentencia del almac�n.
	 * @return Una lista enlazada de strings sin repeticiones que son sujeto y a la vez objeto en el almac�n
	 */
	public ListaEnlazada<String> entidadesSujetoObjeto() {
		ListaEnlazada<String> coleccionEntidades = new ListaEnlazada<String>();
		for( int i = 0; i < nodosSalientes.size(); i++ )
			if( !nodosSalientes.get(i).isEmpty() && !nodosEntrantes.get(i).isEmpty() )
				coleccionEntidades.insertLast( listaSujetosObjetos.get(i) );
		
		return coleccionEntidades;
	}
	
	/**
	 * 5) Colecci�n de entidades que son sujeto en todos y cada uno de los almacenes.
	 * @param coleccionAlmacenes - almacenes a intersectar
	 * @return Una lista enlazada de entidades que son sujeto en todos y cada uno de los almacenes
	 */
	public static ListaEnlazada<String> entidadesSujetoEnTodos(Almacen[] coleccionAlmacenes) {
		ListaEnlazada<String> resultado = new ListaEnlazada<String>();
		int menor = 0, i, j, valor;
		boolean posibleSujetoComun;
		String sujeto;

		// Se busca el �ndice del almac�n que tiene menor n�mero de sujetos
		for (i = 0; i < coleccionAlmacenes.length; i++)
			if (coleccionAlmacenes[i].sujetos < coleccionAlmacenes[menor].sujetos)
				menor = i;

		for (i = 0; i < coleccionAlmacenes[menor].nodosSalientes.size(); i++) {
			// Recorrer todas las entidades del almac�n
			if (!coleccionAlmacenes[menor].nodosSalientes.get(i).isEmpty()) {
				// En caso de ser un sujeto, se busca en el resto de los almacenes
				sujeto = coleccionAlmacenes[menor].listaSujetosObjetos.get(i);
				posibleSujetoComun = true;
				j = 0;
				while (j < coleccionAlmacenes.length && posibleSujetoComun) {
					valor = coleccionAlmacenes[j].arbolSujetosObjetos.obtenerValor(sujeto);
					if (valor == -1 || coleccionAlmacenes[j].nodosSalientes.get(valor).isEmpty()) {
						// No existe o no es sujeto en el almac�n j -> se deja de buscar
						posibleSujetoComun = false;
					}
					j++;
				}

				if (posibleSujetoComun) {
					// Es sujeto en todos los almacenes
					resultado.insertLast(sujeto);
				}
			}
		}

		return resultado;
	}
	
	/**
	 * 6) Colecci�n ordenada de todas las sentencias que aparecen en el almac�n.
	 * @return Un array con las sentencias del almac�n seg�n el orden descrito en el enunciado
	 */
	public ListaArray<String> sentenciasOrdenadas() {
		ListaArray<Integer> valores = arbolSujetosObjetos.recorrerEnProfundidad();
		ListaArray<String> sentenciasEnOrden = new ListaArray<String>(sentencias);
		int nodo;
		Arista arista;
		for( int i = 0; i < valores.size(); ++i ) {
			nodo = valores.get(i);
			for( int j = 0; j < nodosSalientesOrdenados.get(nodo).size(); ++j ) {
				arista = nodosSalientes.get(nodo).get( nodosSalientesOrdenados.get(nodo).get(j) );
				for( int k = 0; k < arista.repeticiones; ++k )
					sentenciasEnOrden.insertLast( listaSujetosObjetos.get(nodo) + " " + listaPropiedades.get(arista.propiedad) + " " + listaSujetosObjetos.get(arista.verticeObjetivo) + " ." );
			}
		}
		return sentenciasEnOrden;
	}
	
	/**
	 * 7a) Colecci�n de las clases de un sujeto, dado como par�metro.
	 * @param sujeto - sujeto del que se buscan las clases
	 * @return una lista enlazada de las clases del par�metro sujeto
	 */
	public ListaEnlazada<String> clasesDe(String sujeto) {
		ListaEnlazada<String> resultado = new ListaEnlazada<String>();
		int idSujeto = arbolSujetosObjetos.obtenerValor(sujeto);
		if (idSujeto != -1) {
			boolean recorridos[] = new boolean[sujetos+objetos];
			for( int i = 0; i < sujetos+objetos; ++i )
				recorridos[i] = false;
			// B�squeda en profundidad
			DFS( idSujeto, identificadorPropiedadEs, recorridos, resultado );
		}
		return resultado;
	}
	
	/**
	 * 7b) Colecci�n de las clases que son superclase de una clase, dada como par�metro.
	 * @param clase - clase de la que se buscan las superclases
	 * @return una lista enlazada de las superclases del par�metro clase
	 */
	public ListaEnlazada<String> superClasesDe(String clase) {
		ListaEnlazada<String> resultado = new ListaEnlazada<String>();
		int idClase = arbolSujetosObjetos.obtenerValor(clase);
		if (idClase != -1) {
			boolean recorridos[] = new boolean[sujetos+objetos];
			for( int i = 0; i < sujetos+objetos; ++i )
				recorridos[i] = false;
			// B�squeda en profundidad
			DFS( idClase, identificadorPropiedadSubClaseDe, recorridos, resultado );
		}
		return resultado;
	}
	
	/**
	 * 8) Colecci�n de entidades que son de una determinada clase, dada como par�metro.
	 * @param clase - clase de la que se buscan las entidades
	 * @return una lista enlazada de las entidades que son del par�metro clase
	 */
	public ListaEnlazada<String> entidadesDeClase(String clase) {
		ListaEnlazada<String> resultado = new ListaEnlazada<String>();
		int idClase = arbolSujetosObjetos.obtenerValor(clase);
		if (idClase != -1) {
			boolean recorridos[] = new boolean[sujetos+objetos];
			for( int i = 0; i < sujetos+objetos; ++i )
				recorridos[i] = false;
			// B�squeda en profundidad hacia atr�s
			DFSinversa( idClase, recorridos, resultado );
		}
		return resultado;
	}
	
	/**
	 * 9a) Cargar sentencias en un almac�n, tomadas de archivos de texto de nuestro directorio.
	 * @param nombreDeArchivo - fichero del que se toman las sentencias para el almac�n
	 * @return un almac�n de sentencias a partir del contenido del fichero
	 */
	public static Almacen cargar(String nombreDeArchivo) {
		try {
			return new Almacen(nombreDeArchivo);
		} catch (IOException e) {
			System.err.println("Error: Imposible acceder al fichero especificado.");
			return null;
		}
	}
	
	/**
	 * 9b) Descargar las sentencias de un almac�n en un archivo de texto de nuestro directorio.
	 * @param nombreDeArchivo - fichero en el que se van a escribir las sentencias del almac�n
	 */
	public void descargar(String nombreDeArchivo) {
		try {
			Arista arista;
			Fichero.abrir(nombreDeArchivo, true, false);
			for (int i = 0; i < nodosSalientes.size(); ++i) {
				for (int j = 0; j < nodosSalientes.get(i).size(); ++j) {
					arista = nodosSalientes.get(i).get(j);
					for (int k = 0; k < arista.repeticiones; ++k)
						Fichero.escribirSentencia( listaSujetosObjetos.get(i) + " " + listaPropiedades.get(arista.propiedad) + " " + listaSujetosObjetos.get(arista.verticeObjetivo) + " ." );
				}
			}
			Fichero.cerrar();
		} catch (IOException e) {
			System.err.println("Error: Imposible acceder al fichero especificado.");
			return;
		}
	}
	
	
	// B�squeda en profundidad desde "nodo" recorriendo solo las aristas de peso "propiedad"
	// Los nodos que se recorren se guardan en "resultado"
	private void DFS( int nodo, int propiedad, boolean[] recorridos, ListaEnlazada<String> resultado ) {
		Arista a;
		for (int i = 0; i < nodosSalientes.get(nodo).size(); ++i) {
			a = nodosSalientes.get(nodo).get(i);
			if (a.propiedad == propiedad && !recorridos[a.verticeObjetivo]) {
				recorridos[a.verticeObjetivo] = true;
				resultado.insertLast(listaSujetosObjetos.get(a.verticeObjetivo));
				DFS(a.verticeObjetivo, identificadorPropiedadSubClaseDe, recorridos, resultado);
			}
		}
	}
	
	// B�squeda en profundidad hacia atr�s desde "nodo", recorriendo por nodosEntrantes
	// Se lanza la b�squeda otra vez si el peso de la arista es 'subClaseDe', o se a�ade
	// el nodo al resultado si el peso es 'es'
	private void DFSinversa( int nodo, boolean[] recorridos, ListaEnlazada<String> resultado ) {
		Arista a;
		for (int i = 0; i < nodosEntrantes.get(nodo).size(); ++i) {
			a = nodosEntrantes.get(nodo).get(i);
			if (a.propiedad == identificadorPropiedadEs && !recorridos[a.verticeObjetivo]) {
				recorridos[a.verticeObjetivo] = true;
				resultado.insertLast(listaSujetosObjetos.get(a.verticeObjetivo));
			} else if (a.propiedad == identificadorPropiedadSubClaseDe && !recorridos[a.verticeObjetivo]) {
				recorridos[a.verticeObjetivo] = true;
				DFSinversa(a.verticeObjetivo, recorridos, resultado);
			}
		}
	}
	
}
