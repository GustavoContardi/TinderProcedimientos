import { Routes } from '@angular/router';
import { authGuard, rolGuard } from './core/guards';

const registro = () => import('./pages/registro/registro.component').then((m) => m.RegistroComponent);

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'swipe' },
      { path: 'swipe', loadComponent: () => import('./pages/votar/votar.component').then((m) => m.VotarComponent) },
      { path: 'resultados', loadComponent: () => import('./pages/resultados/resultados.component').then((m) => m.ResultadosComponent) },
      { path: 'casamientos', loadComponent: registro, data: { pestania: 'casamientos' } },
      { path: 'velorios', loadComponent: registro, data: { pestania: 'velorios' } },
      { path: 'archivo', loadComponent: registro, data: { pestania: 'archivo' } },
      { path: 'aniversarios', canActivate: [rolGuard('JUEZ_PAZ', 'DIRECCION')], loadComponent: registro, data: { pestania: 'aniversarios' } },
      { path: 'tablero', loadComponent: () => import('./pages/tablero/tablero.component').then((m) => m.TableroComponent) },
      {
        path: 'mis-procedimientos',
        loadComponent: () => import('./pages/mis-procedimientos/mis-procedimientos.component').then((m) => m.MisProcedimientosComponent),
      },
      {
        path: 'juez-de-paz',
        canActivate: [rolGuard('JUEZ_PAZ', 'DIRECCION')],
        loadComponent: () => import('./pages/juez-de-paz/juez-de-paz.component').then((m) => m.JuezDePazComponent),
      },
      {
        path: 'matriz',
        canActivate: [rolGuard('JUEZ_PAZ')],
        loadComponent: () => import('./pages/matriz/matriz.component').then((m) => m.MatrizComponent),
      },
      {
        path: 'objeciones',
        canActivate: [rolGuard('JUEZ_PAZ')],
        loadComponent: () => import('./pages/objeciones/objeciones.component').then((m) => m.ObjecionesComponent),
      },
      {
        path: 'segundas-citas',
        canActivate: [rolGuard('JUEZ_PAZ')],
        loadComponent: () => import('./pages/segundas-citas/segundas-citas.component').then((m) => m.SegundasCitasComponent),
      },
      {
        path: 'gestion',
        canActivate: [rolGuard('JUEZ_PAZ')],
        loadComponent: () => import('./pages/gestion/gestion.component').then((m) => m.GestionComponent),
      },
      // Rutas viejas
      { path: 'votar', redirectTo: 'swipe' },
      { path: 'registro', redirectTo: 'casamientos' },
      { path: 'procedimientos', redirectTo: 'gestion' },
      { path: 'ciclos', redirectTo: 'gestion' },
    ],
  },
  { path: '**', redirectTo: '' },
];
