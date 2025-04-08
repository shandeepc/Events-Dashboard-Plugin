import { Component, Input, isDevMode } from '@angular/core';
import { EventDetail, EventService } from '../services/event.service';

declare const SailPoint: {
  CONTEXT_PATH: string
};

@Component({
  selector: 'app-event-full-view',
  templateUrl: './event-full-view.component.html',
  styleUrls: ['./event-full-view.component.css']
})

export class EventFullViewComponent {
  @Input() eventName!: string;
  @Input() eventData!: EventDetail[];
  isVisible = false;

  private iiqUrl: string;

  constructor(private eventService: EventService) {
    if (isDevMode()) {
      this.iiqUrl = 'http://sp.com:8080'; // use proxy
    } else {
      this.iiqUrl = SailPoint.CONTEXT_PATH;
    }
  }

  show(): void {
    console.log('show');
    this.isVisible = true;
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
  }

  close(): void {
    console.log('close');
    this.isVisible = false;
    document.body.style.overflow = ''; // Restore default scrolling
  }

  openIdentityRequest(identityRequestId: string): void {
    const url = `${this.iiqUrl}/identityRequest/identityRequest.jsf#/request/${identityRequestId}`;
    window.open(url, '_blank');
  }

  openIdentity(targetId: string): void {
    const url = `${this.iiqUrl}/define/identity/identity.jsf?id=${targetId}`;
    window.open(url, '_blank');
  }
  pageLimit: number = 2;
  pageNumber: number = 1;
  hasMorePages: boolean = true;
  dateFilter: string = 'today';
  startDate: string = new Date().toISOString().split('T')[0];
  endDate: string = new Date().toISOString().split('T')[0];
  isDownloading: boolean = false;

  onDateFilterChange() {
    const today = new Date();
    switch (this.dateFilter) {
      case 'today':
        this.startDate = today.toISOString().split('T')[0];
        this.endDate = today.toISOString().split('T')[0];
        break;
      case 'yesterday':
        const yesterday = new Date(today);
        yesterday.setDate(today.getDate() - 1);
        this.startDate = yesterday.toISOString().split('T')[0];
        this.endDate = yesterday.toISOString().split('T')[0];
        break;
      case 'last4days':
        const fourDaysAgo = new Date(today);
        fourDaysAgo.setDate(today.getDate() - 4);
        this.startDate = fourDaysAgo.toISOString().split('T')[0];
        this.endDate = today.toISOString().split('T')[0];
        break;
      case 'last7days':
        const sevenDaysAgo = new Date(today);
        sevenDaysAgo.setDate(today.getDate() - 7);
        this.startDate = sevenDaysAgo.toISOString().split('T')[0];
        this.endDate = today.toISOString().split('T')[0];
        break;
      case 'all':
        const infinity = new Date();
        infinity.setFullYear(1970);
        infinity.setMonth(0);
        infinity.setDate(1);
        this.startDate = infinity.toISOString().split('T')[0];
        this.endDate = today.toISOString().split('T')[0];
        break;
    }
  }

  applyFilters() {
    this.pageNumber = 1;
    console.log('Applying Filters..');
    console.log("eventName - " + this.eventName);
    console.log("pageLimit - " + this.pageLimit);
    console.log("pageNumber - " + this.pageNumber);
    console.log("startDate - " + this.startDate);
    console.log("endDate - " + this.endDate);
    this.fetchEventData();
  }

  previousPage() {
    if (this.pageNumber > 1) {
      this.pageNumber--;
      this.fetchEventData();
    }
  }

  nextPage() {
    this.pageNumber++;
    this.fetchEventData();
  }

  fetchEventData() {
    this.eventService.getEventWithFilter(this.eventName, this.pageLimit, this.pageNumber, this.startDate, this.endDate
    ).subscribe(response => {
      //console.log(response);
      this.eventData = response[this.eventName];
      console.log('data length - ' + this.eventData.length);
      console.log('this.pageLimit - ' + this.pageLimit);
      this.hasMorePages = this.eventData.length == this.pageLimit;
      console.log("hasMorePages? - " + this.hasMorePages);
    });
  }

  downloadEvents() {
    this.isDownloading = true;
    this.eventService.getEventWithFilter(this.eventName, 0, 0, this.startDate, this.endDate)
      .subscribe({
        next: (data) => {
          const eventData = data[this.eventName];
          if (!eventData || eventData.length === 0) {
            alert('Data not available for selected Filters, Try different filter');
            this.isDownloading = false;
            return;
          }
          const csvContent = this.eventService.convertToCSV(eventData);
          this.eventService.downloadCSV(csvContent, `${this.eventName}_events.csv`);
          this.isDownloading = false;
        },
        error: (error) => {
          console.error('Error downloading events:', error);
          this.isDownloading = false;
        }
      });
  }
}
