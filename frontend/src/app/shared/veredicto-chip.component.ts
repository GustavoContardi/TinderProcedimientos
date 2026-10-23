import { Component, computed, input } from '@angular/core';
import { Veredicto } from '../core/models';
import { VEREDICTOS } from '../core/etiquetas';

@Component({
  selector: 'app-veredicto-chip',
  template: `<span class="chip" [class]="'chip ' + info().clase" [title]="titulo()">{{ info().texto }}</span>`,
})
export class VeredictoChipComponent {
  readonly veredicto = input.required<Veredicto>();
  readonly info = computed(() => VEREDICTOS[this.veredicto()]);
  readonly titulo = computed(() =>
    this.veredicto() === 'QUORUM_INSUFICIENTE' ? 'Pocos votos, por anonimato no mostramos números' : this.info().texto,
  );
}
