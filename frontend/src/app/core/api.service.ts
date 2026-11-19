import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Alerta,
  Aniversario,
  Archivo,
  Area,
  Calendario,
  Casamiento,
  CasamientoRequest,
  Celda,
  Ciclo,
  ContenidoRequest,
  DecisionSegundaCita,
  Designacion,
  EstadoProcedimiento,
  GrupoObjeciones,
  Matriz,
  Mejora,
  MejoraUpdate,
  Objecion,
  ObjecionNueva,
  OrigenObjecion,
  Perfil,
  Procedimiento,
  ProcedimientoRequest,
  ResultadoProcedimiento,
  ResumenCierre,
  Rol,
  SegundaCita,
  SugerenciaVelorio,
  Usuario,
  ValorVoto,
  Velorio,
  VelorioRequest,
} from './models';

const API = '/api';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Tinder
  mazo(): Observable<Perfil[]> {
    return this.http.get<Perfil[]>(`${API}/tinder/mazo`);
  }
  votar(procedimientoId: number, cicloId: number, valor: ValorVoto, objecion: ObjecionNueva | null): Observable<{ match: boolean }> {
    return this.http.post<{ match: boolean }>(`${API}/tinder/votos`, { procedimientoId, cicloId, valor, objecion });
  }

  // Ciclos
  cicloActual(): Observable<Ciclo | null> {
    return this.http.get<Ciclo | null>(`${API}/ciclos/actual`);
  }
  ciclos(): Observable<Ciclo[]> {
    return this.http.get<Ciclo[]>(`${API}/ciclos`);
  }
  abrirCiclo(body: { nombre: string; fechaInicio: string; fechaFin: string }): Observable<Ciclo> {
    return this.http.post<Ciclo>(`${API}/ciclos`, body);
  }
  cerrarCiclo(id: number): Observable<ResumenCierre> {
    return this.http.post<ResumenCierre>(`${API}/ciclos/${id}/cerrar`, {});
  }

  // Resultados
  resultados(cicloId?: number | null, areaId?: number | null): Observable<ResultadoProcedimiento[]> {
    let params = new HttpParams();
    if (cicloId) params = params.set('cicloId', cicloId);
    if (areaId) params = params.set('areaId', areaId);
    return this.http.get<ResultadoProcedimiento[]>(`${API}/resultados`, { params });
  }

  // Procedimientos
  procedimientos(estado?: EstadoProcedimiento | null): Observable<Procedimiento[]> {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<Procedimiento[]>(`${API}/procedimientos`, { params });
  }
  misProcedimientos(): Observable<Procedimiento[]> {
    return this.http.get<Procedimiento[]>(`${API}/procedimientos/mios`);
  }
  archivo(): Observable<Archivo[]> {
    return this.http.get<Archivo[]>(`${API}/procedimientos/archivo`);
  }
  crearProcedimiento(body: ProcedimientoRequest): Observable<Procedimiento> {
    return this.http.post<Procedimiento>(`${API}/procedimientos`, body);
  }
  actualizarProcedimiento(id: number, body: ProcedimientoRequest): Observable<Procedimiento> {
    return this.http.put<Procedimiento>(`${API}/procedimientos/${id}`, body);
  }
  editarContenido(id: number, body: ContenidoRequest): Observable<Procedimiento> {
    return this.http.put<Procedimiento>(`${API}/procedimientos/${id}/contenido`, body);
  }
  publicar(id: number): Observable<Procedimiento> {
    return this.http.post<Procedimiento>(`${API}/procedimientos/${id}/publicar`, {});
  }
  eliminarProcedimiento(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/procedimientos/${id}`);
  }

  // Matriz
  matriz(areaId?: number | null): Observable<Matriz> {
    const params = areaId ? new HttpParams().set('areaId', areaId) : undefined;
    return this.http.get<Matriz>(`${API}/matriz`, { params });
  }
  guardarMatriz(altas: Celda[], bajas: Celda[]): Observable<{ altas: number; bajas: number; mensaje: string }> {
    return this.http.put<{ altas: number; bajas: number; mensaje: string }>(`${API}/matriz`, { altas, bajas });
  }

  // Objeciones
  objeciones(procedimientoId?: number | null): Observable<GrupoObjeciones[]> {
    const params = procedimientoId ? new HttpParams().set('procedimientoId', procedimientoId) : undefined;
    return this.http.get<GrupoObjeciones[]>(`${API}/objeciones`, { params });
  }
  evaluarObjecion(id: string, body: { fundada: boolean; respuesta: string | null; fechaCompromiso: string | null }): Observable<Objecion> {
    return this.http.patch<Objecion>(`${API}/objeciones/${id}`, body);
  }

  // Segundas citas
  segundasCitas(todas = false): Observable<SegundaCita[]> {
    const params = todas ? new HttpParams().set('todas', true) : undefined;
    return this.http.get<SegundaCita[]>(`${API}/segundas-citas`, { params });
  }
  realizarCita(
    id: number,
    body: { minuta: string; decision: DecisionSegundaCita; condiciones: string | null; lanzarRapida: boolean },
  ): Observable<SegundaCita> {
    return this.http.put<SegundaCita>(`${API}/segundas-citas/${id}`, body);
  }

  // Casamientos
  casamientos(): Observable<Casamiento[]> {
    return this.http.get<Casamiento[]>(`${API}/casamientos`);
  }
  casamiento(id: number): Observable<Casamiento> {
    return this.http.get<Casamiento>(`${API}/casamientos/${id}`);
  }
  casar(body: CasamientoRequest): Observable<Casamiento> {
    return this.http.post<Casamiento>(`${API}/casamientos`, body);
  }
  objetarCasamiento(id: number, body: ObjecionNueva): Observable<{ origen: OrigenObjecion; mensaje: string }> {
    return this.http.post<{ origen: OrigenObjecion; mensaje: string }>(`${API}/casamientos/${id}/objeciones`, body);
  }
  aniversarios(): Observable<Aniversario[]> {
    return this.http.get<Aniversario[]>(`${API}/casamientos/aniversarios`);
  }
  accionAniversario(id: number, accion: 'renovar' | 'segunda-cita' | 'divorciar', usoReal: string | null): Observable<Casamiento> {
    return this.http.post<Casamiento>(`${API}/casamientos/${id}/${accion}`, { usoReal });
  }

  // Velorios
  velorios(): Observable<Velorio[]> {
    return this.http.get<Velorio[]>(`${API}/velorios`);
  }
  velorio(id: number): Observable<Velorio> {
    return this.http.get<Velorio>(`${API}/velorios/${id}`);
  }
  sugerenciaVelorio(procedimientoId: number): Observable<SugerenciaVelorio> {
    return this.http.get<SugerenciaVelorio>(`${API}/velorios/sugerencia`, { params: { procedimientoId } });
  }
  crearVelorio(body: VelorioRequest): Observable<Velorio> {
    return this.http.post<Velorio>(`${API}/velorios`, body);
  }
  firmarActa(id: number): Observable<Velorio> {
    return this.http.post<Velorio>(`${API}/velorios/${id}/firmar`, {});
  }

  // Tablero de mejoras
  mejoras(): Observable<Mejora[]> {
    return this.http.get<Mejora[]>(`${API}/mejoras`);
  }
  actualizarMejora(id: number, body: MejoraUpdate): Observable<Mejora> {
    return this.http.put<Mejora>(`${API}/mejoras/${id}`, body);
  }

  // Juez de paz
  designaciones(): Observable<Designacion[]> {
    return this.http.get<Designacion[]>(`${API}/juez-de-paz/designaciones`);
  }
  designar(usuarioId: number, areaId: number): Observable<Designacion> {
    return this.http.post<Designacion>(`${API}/juez-de-paz/designaciones`, { usuarioId, areaId });
  }
  calendario(): Observable<Calendario> {
    return this.http.get<Calendario>(`${API}/juez-de-paz/calendario`);
  }

  // Catálogos y alertas
  areas(): Observable<Area[]> {
    return this.http.get<Area[]>(`${API}/areas`);
  }
  usuarios(rol?: Rol): Observable<Usuario[]> {
    const params = rol ? new HttpParams().set('rol', rol) : undefined;
    return this.http.get<Usuario[]>(`${API}/usuarios`, { params });
  }
  alertas(): Observable<Alerta[]> {
    return this.http.get<Alerta[]>(`${API}/alertas`);
  }
  revisarAlerta(id: number): Observable<Alerta> {
    return this.http.post<Alerta>(`${API}/alertas/${id}/revisar`, {});
  }
}
