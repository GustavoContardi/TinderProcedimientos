import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Alerta,
  Area,
  Casamiento,
  CasamientoRequest,
  Ciclo,
  EstadoProcedimiento,
  Perfil,
  Procedimiento,
  ProcedimientoRequest,
  Resultado,
  ResumenCierre,
  Rol,
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
  votar(procedimientoId: number, valor: ValorVoto): Observable<{ match: boolean }> {
    return this.http.post<{ match: boolean }>(`${API}/tinder/votos`, { procedimientoId, valor });
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
  resultados(cicloId?: number | null, areaId?: number | null): Observable<Resultado[]> {
    let params = new HttpParams();
    if (cicloId) params = params.set('cicloId', cicloId);
    if (areaId) params = params.set('areaId', areaId);
    return this.http.get<Resultado[]>(`${API}/resultados`, { params });
  }

  // Procedimientos
  procedimientos(estado?: EstadoProcedimiento | null): Observable<Procedimiento[]> {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<Procedimiento[]>(`${API}/procedimientos`, { params });
  }
  crearProcedimiento(body: ProcedimientoRequest): Observable<Procedimiento> {
    return this.http.post<Procedimiento>(`${API}/procedimientos`, body);
  }
  actualizarProcedimiento(id: number, body: ProcedimientoRequest): Observable<Procedimiento> {
    return this.http.put<Procedimiento>(`${API}/procedimientos/${id}`, body);
  }
  eliminarProcedimiento(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/procedimientos/${id}`);
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
  aniversarios(): Observable<Casamiento[]> {
    return this.http.get<Casamiento[]>(`${API}/casamientos/aniversarios`);
  }
  renovar(id: number): Observable<Casamiento> {
    return this.http.post<Casamiento>(`${API}/casamientos/${id}/renovar`, {});
  }
  divorciar(id: number): Observable<Casamiento> {
    return this.http.post<Casamiento>(`${API}/casamientos/${id}/divorciar`, {});
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
