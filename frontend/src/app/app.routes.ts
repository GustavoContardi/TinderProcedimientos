import { Routes } from '@angular/router';
import { authGuard, rolGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'votar' },
      { path: 'votar', loadComponent: () => import('./pages/votar/votar.component').then((m) => m.VotarComponent) },
      { path: 'resultados', loadComponent: () => import('./pages/resultados/resultados.component').then((m) => m.ResultadosComponent) },
      { path: 'registro', loadComponent: () => import('./pages/registro/registro.component').then((m) => m.RegistroComponent) },
      {
        path: 'gestion',
        canActivate: [rolGuard('CALIDAD')],
        loadComponent: () => import('./pages/gestion/gestion.component').then((m) => m.GestionComponent),
      },
      // Rutas de la versión anterior
      { path: 'swipe', redirectTo: 'votar' },
      { path: 'casamientos', redirectTo: 'registro' },
      { path: 'velorios', redirectTo: 'registro' },
      { path: 'aniversarios', redirectTo: 'registro' },
      { path: 'procedimientos', redirectTo: 'gestion' },
      { path: 'ciclos', redirectTo: 'gestion' },
    ],
  },
  { path: '**', redirectTo: '' },
];
