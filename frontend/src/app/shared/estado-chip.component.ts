import { Component, computed, input } from '@angular/core';
import { EstadoProcedimiento } from '../core/models';
import { ESTADOS } from '../core/etiquetas';

@Component({
  selector: 'app-estado-chip',
  template: `<span [class]="'chip ' + info().clase">{{ info().icono }} {{ info().texto }}</span>`,
})
export class EstadoChipComponent {
  readonly estado = input.required<EstadoProcedimiento>();
  readonly info = computed(() => ESTADOS[this.estado()]);
}
