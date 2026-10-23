import { Routes } from '@angular/router';
import { authGuard, rolGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'swipe' },
      { path: 'swipe', loadComponent: () => import('./pages/swipe/swipe.component').then((m) => m.SwipeComponent) },
      { path: 'resultados', loadComponent: () => import('./pages/resultados/resultados.component').then((m) => m.ResultadosComponent) },
      {
        path: 'procedimientos',
        canActivate: [rolGuard('CALIDAD')],
        loadComponent: () => import('./pages/procedimientos/procedimientos.component').then((m) => m.ProcedimientosComponent),
      },
      {
        path: 'ciclos',
        canActivate: [rolGuard('CALIDAD')],
        loadComponent: () => import('./pages/ciclos/ciclos.component').then((m) => m.CiclosComponent),
      },
      { path: 'casamientos', loadComponent: () => import('./pages/casamientos/casamientos.component').then((m) => m.CasamientosComponent) },
      { path: 'casamientos/:id', loadComponent: () => import('./pages/casamientos/libreta.component').then((m) => m.LibretaComponent) },
      { path: 'velorios', loadComponent: () => import('./pages/velorios/velorios.component').then((m) => m.VeloriosComponent) },
      { path: 'velorios/:id', loadComponent: () => import('./pages/velorios/necrologica.component').then((m) => m.NecrologicaComponent) },
      {
        path: 'aniversarios',
        canActivate: [rolGuard('CALIDAD', 'DIRECCION')],
        loadComponent: () => import('./pages/aniversarios/aniversarios.component').then((m) => m.AniversariosComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
