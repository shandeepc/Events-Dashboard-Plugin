import { Component, OnInit } from '@angular/core';
import { EventService } from './services/event.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  events: string[] = [];
  eventDetails: { [key: string]: any[] } = {};
  loadingStates: { [key: string]: boolean } = {};
  isLoading: boolean = true;
  selectedEvent: string | null = null;

  constructor(private eventService: EventService) {}

  ngOnInit() {

    const headerElement = document.querySelector<HTMLElement>('#spHeaderPanelDiv');
    if (headerElement) {
      //headerElement.remove();
      headerElement.hidden = true;
    }

    this.eventService.getUniqueEvents().subscribe(response => {
      this.events = response.Events;
      
      // Initialize loading states for each event
      this.events.forEach(event => {
        this.eventDetails[event] = []; // Initialize with empty array
        this.loadingStates[event] = true; // Set initial loading state
        this.eventService.getAllEvents(event, 15).subscribe(result => {
          this.eventDetails[event] = Object.values(result)[0];
          this.loadingStates[event] = false; // Set loading to false when data is loaded
        });
      });
      this.isLoading = false;
    });
  }

  onEventSelected(eventName: string) {
    this.selectedEvent = this.selectedEvent === eventName ? null : eventName;
  }

  isEventSelected(eventName: string): boolean {
    return this.selectedEvent === eventName;
  }

  isEventLoading(eventName: string): boolean {
    return this.loadingStates[eventName] || false;
  }
}
