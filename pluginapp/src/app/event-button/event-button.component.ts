import { Component, Input, Output, EventEmitter, isDevMode } from '@angular/core';

@Component({
  selector: 'app-event-button',
  templateUrl: './event-button.component.html',
  styleUrls: ['./event-button.component.css']
})
export class EventButtonComponent {
  @Input() eventName: string = '';
  @Input() isSelected: boolean = false;
  @Output() eventSelected = new EventEmitter<string>();

  onEventClick() {
    this.eventSelected.emit(this.eventName);

  }

  isD() {
    return isDevMode();
  }
}
